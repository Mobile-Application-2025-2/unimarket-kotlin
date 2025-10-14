package com.example.unimarket.view.explore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.controller.explore.ExploreBuyerController
import com.example.unimarket.controller.explore.ExploreBuyerViewPort
import com.example.unimarket.model.entity.Category
import com.example.unimarket.model.repository.ExploreRepository

class ExploreBuyerActivity : ComponentActivity(), ExploreBuyerViewPort {

    private lateinit var controller: ExploreBuyerController

    // Estado que muestra la vista (Compose)
    private val chipsState = mutableStateListOf<String>()
    private var selectedChip by mutableStateOf(0)
    private val itemsState = mutableStateListOf<Category>()
    private var loading by mutableStateOf(false)
    private var lastError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // INYECTAR EL REPO (definido en model.repository)
        val repository: ExploreRepository =
            TODO("Proveer ExploreRepository real, p.ej. ExploreRepositoryImpl(api)")

        // CREA EL CONTROLLER (definido en controller.explore)
        controller = ExploreBuyerController(this, repository)

        setContent {
            ExploreBuyerScreen(
                chips = chipsState,
                selectedIndex = selectedChip,
                items = itemsState,
                loading = loading,
                error = lastError,
                onSelectChip = { idx -> selectedChip = idx; controller.onChipSelected(idx) },
                onRetry = { controller.onRefresh() }
            )
        }

        controller.onInit()
    }

    override fun showLoading(show: Boolean) { loading = show }
    override fun showError(message: String) { lastError = message }
    override fun renderChips(labels: List<String>) {
        chipsState.clear(); chipsState.addAll(labels)
        selectedChip = 0
    }
    override fun renderCategories(list: List<Category>) {
        itemsState.clear(); itemsState.addAll(list)
        lastError = null
    }
}

private val Accent = Color(0xFFF7B500)
private val Pastels = listOf(
    Color(0xFFFFF1E0),
    Color(0xFFEFFBF0),
    Color(0xFFF7EDFF),
    Color(0xFFFFEFEF),
    Color(0xFFFFF8D9),
    Color(0xFFEAF5FF)
)

private data class CatChip(val label: String, val icon: ImageVector?)
private val iconMap = mapOf(
    "Todos" to Icons.Outlined.AllInbox,
    "Tutorías" to Icons.Outlined.School,
    "Comida" to Icons.Outlined.Fastfood,
    "Emprendimiento" to Icons.Outlined.TrendingUp,
    "Papelería" to Icons.Outlined.Edit,
    "Otro" to Icons.Outlined.Category
)

@Composable
fun ExploreBuyerScreen(
    chips: List<String>,
    selectedIndex: Int,
    items: List<Category>,
    loading: Boolean,
    error: String?,
    onSelectChip: (Int) -> Unit,
    onRetry: () -> Unit
) {
    val chipModels = remember(chips) { chips.map { CatChip(it, iconMap[it]) } }

    Scaffold(
        topBar = { ExploreTopBar() },
        bottomBar = { BuyerBottomBar(current = 1, onClick = { }) },
        containerColor = Color.White
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SearchBox()
            Spacer(Modifier.height(8.dp))

            if (chips.isNotEmpty()) {
                CategoryChipsRow(
                    chips = chipModels,
                    selectedIndex = selectedIndex,
                    onSelect = onSelectChip
                )
                Spacer(Modifier.height(8.dp))
            }

            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("Reintentar") }
                    }
                }
                else -> {
                    val rows = remember(items) { items.chunked(2) }
                    rows.forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { cat ->
                                ExploreCard(
                                    title = cat.name,
                                    type = cat.type,
                                    selectionCount = cat.selectionCount,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(160.dp)
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Spacer(Modifier.height(72.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛍️ ", fontSize = 22.sp)
                Text("Explorar", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
        },
        actions = {
            IconButton(onClick = {}) { Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favoritos") }
            IconButton(onClick = {}) { Icon(Icons.Outlined.LocalShipping, contentDescription = "Envíos") }
        }
    )
}

@Composable
private fun SearchBox() {
    TextField(
        value = "",
        onValueChange = {},
        placeholder = { Text("Buscar en UniMarket") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        readOnly = true,
        shape = RoundedCornerShape(26.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF2F4F7),
            unfocusedContainerColor = Color(0xFFF2F4F7),
            disabledContainerColor = Color(0xFFF2F4F7),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun CategoryChipsRow(
    chips: List<CatChip>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        itemsIndexed(chips) { i, c ->
            FilterChip(
                selected = i == selectedIndex,
                onClick = { onSelect(i) },
                label = { Text(c.label, fontWeight = FontWeight.SemiBold) },
                leadingIcon = c.icon?.let {
                    { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) }
                },
                shape = RoundedCornerShape(18.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = Color(0xFFF7F7F7),
                    labelColor = Color(0xFF333333)
                )
            )
        }
    }
}

@Composable
private fun BuyerBottomBar(current: Int, onClick: (Int) -> Unit) {
    NavigationBar(containerColor = Accent) {
        val items = listOf(
            Icons.Outlined.Storefront to "Inicio",
            Icons.Outlined.Search to "Buscar",
            Icons.Outlined.ShoppingCart to "Carrito",
            Icons.Outlined.Person to "Perfil"
        )
        items.forEachIndexed { idx, (icon, _) ->
            NavigationBarItem(
                selected = idx == current,
                onClick = { onClick(idx) },
                icon = { Icon(icon, contentDescription = null) },
                label = null,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.8f),
                    indicatorColor = Color.White.copy(alpha = 0.18f)
                )
            )
        }
    }
}

@Composable
private fun ExploreCard(
    title: String,
    type: String,
    selectionCount: Int,
    modifier: Modifier = Modifier
) {
    val Pastels = listOf(
        Color(0xFFFFF1E0),
        Color(0xFFEFFBF0),
        Color(0xFFF7EDFF),
        Color(0xFFFFEFEF),
        Color(0xFFFFF8D9),
        Color(0xFFEAF5FF)
    )
    val bg = Pastels[(title.hashCode() xor type.hashCode()).absoluteValue % Pastels.size]
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = bg,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Placeholder visual (sin imágenes del modelo)
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.6f))
            )
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("Popularidad: $selectionCount", fontSize = 12.sp, color = Color(0xFF5F6368))
        }
    }
}

private val Int.absoluteValue: Int get() = if (this < 0) -this else this