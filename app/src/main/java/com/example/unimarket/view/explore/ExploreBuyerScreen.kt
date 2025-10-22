package com.example.unimarket.view.explore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.unimarket.R
import androidx.compose.foundation.clickable
import com.example.unimarket.SupaConst
import com.example.unimarket.model.api.AuthApiFactory
import com.example.unimarket.model.repository.ExploreRepository
import com.example.unimarket.model.session.SessionManager
import com.example.unimarket.model.entity.Category
import kotlinx.coroutines.launch

class ExploreBuyerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userJwt = SessionManager.get()?.accessToken
        val categoriesApi = AuthApiFactory.createCategoriesApi(
            baseUrl = SupaConst.SUPABASE_URL,
            anonKey = SupaConst.SUPABASE_ANON_KEY,
            userJwt = userJwt,
            enableLogging = true
        )
        val repo = ExploreRepository(categoriesApi)

        setContent { ExploreBuyerScreen(repo) }
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

private data class ExploreItem(
    val id: String,
    val title: String,
    val type: String,
    val imageUrl: String?,
    @DrawableRes val imageRes: Int?,
    val selectionCount: Int,
    val bg: Color,
    val highlighted: Boolean = false
)

private data class CatChip(val label: String, val icon: ImageVector? = null)
private val catChips = listOf(
    CatChip("Todos", Icons.Outlined.AllInbox),
    CatChip("Tutorías", Icons.Outlined.School),
    CatChip("Comida", Icons.Outlined.Fastfood),
    CatChip("Emprendimiento", Icons.Outlined.TrendingUp),
    CatChip("Papelería", Icons.Outlined.Edit),
    CatChip("Otro", Icons.Outlined.Category)
)

@Composable
fun ExploreBuyerScreen(repo: ExploreRepository) {
    var selectedChip by remember { mutableStateOf(0) }
    var liveItems by remember { mutableStateOf<List<ExploreItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadFromRepo(repo) { items, err ->
            liveItems = items ?: emptyList()
            error = err
            isLoading = false
        }
    }

    val filtered = remember(liveItems, selectedChip) {
        val base = if (selectedChip == 0) liveItems
        else {
            val label = catChips[selectedChip].label
            liveItems.filter { it.type.equals(label, ignoreCase = true) }
        }
        base.sortedByDescending { it.selectionCount }
    }

    Scaffold(
        topBar = { ExploreTopBar() },
        bottomBar = { BuyerBottomBar(current = 1) },
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

            CategoryChipsRow(
                chips = catChips,
                selectedIndex = selectedChip,
                onSelect = { selectedChip = it }
            )

            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            if (!isLoading && error == null && liveItems.isEmpty()) {
                Text(
                    text = "No hay categorías disponibles.",
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            val rows = remember(filtered) { filtered.chunked(2) }
            rows.forEachIndexed { rowIdx, row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { item ->
                        ExploreCard(
                            item = item,
                            modifier = Modifier
                                .weight(1f)
                                .height(160.dp),
                            onClick = {
                                scope.launch {
                                    val prev = liveItems
                                    liveItems = liveItems.map {
                                        if (it.id == item.id) it.copy(selectionCount = it.selectionCount + 1)
                                        else it
                                    }
                                    val res = repo.incrementSelectionCount(item.id, item.selectionCount)
                                    res.onFailure { e ->
                                        liveItems = prev
                                        error = e.message ?: "No se pudo actualizar la categoría."
                                    }
                                }
                            }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                if (rowIdx < rows.lastIndex) Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(72.dp))

            if (!isLoading) {
                TextButton(onClick = {
                    isLoading = true
                    error = null
                    scope.launch {
                        loadFromRepo(repo) { items, err ->
                            liveItems = items ?: emptyList()
                            error = err
                            isLoading = false
                        }
                    }
                }) { Text("Recargar") }
            }
        }
    }
}

private suspend fun loadFromRepo(
    repo: ExploreRepository,
    onDone: (List<ExploreItem>?, String?) -> Unit
) {
    repo.getCategories(order = "selection_count.desc")
        .onSuccess { cats ->
            val mapped = cats.mapIndexed { idx, c -> c.toExploreItem(idx) }
            onDone(mapped, null)
        }
        .onFailure { e ->
            onDone(null, e.message ?: "Error")
        }
}
private fun Category.toExploreItem(idx: Int): ExploreItem {
    val typeHuman = when ((this.type ?: "").lowercase()) {
        "tutoría_matemáticas", "tutoría_idiomas", "tutorías", "tutorias" -> "Tutorías"
        "comida_rápida", "comida_casera", "comida" -> "Comida"
        "venta_funko", "emprendimiento", "venta_varios" -> "Emprendimiento"
        "copias", "papelería", "papeleria" -> "Papelería"
        else -> "Otro"
    }

    val placeholder = when (typeHuman) {
        "Tutorías" -> R.drawable.tutoriasiind2106
        "Comida" -> R.drawable.tacos
        "Emprendimiento" -> R.drawable.funko
        "Papelería" -> R.drawable.papeleria
        else -> R.drawable.papeleria
    }
    val count: Int = runCatching {
        (this::class.members.firstOrNull { it.name == "selection_count" }?.call(this) as? Number)?.toInt()
            ?: (this::class.members.firstOrNull { it.name == "selectionCount" }?.call(this) as? Number)?.toInt()
            ?: 0
    }.getOrDefault(0)

    return ExploreItem(
        id = this.id,
        title = this.name,
        type = typeHuman,
        imageUrl = this.image,
        imageRes = placeholder,
        selectionCount = count,
        bg = Pastels[idx % Pastels.size],
        highlighted = idx < 2
    )
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
private fun ExploreCard(item: ExploreItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = item.bg,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        border = if (item.highlighted) BorderStroke(2.dp, Color(0xFF2F80ED)) else null
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            val context = LocalContext.current
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.9f)),
                    contentScale = ContentScale.Crop,
                    placeholder = item.imageRes?.let { painterResource(it) },
                    error = item.imageRes?.let { painterResource(it) }
                )
            } else {
                Image(
                    painter = painterResource(id = item.imageRes ?: R.drawable.papeleria),
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.9f)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                item.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun BuyerBottomBar(
    current: Int,
    onClick: (Int) -> Unit = {}
) {
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

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_6")
@Composable
private fun PreviewExploreBuyer() {
    Scaffold(topBar = { ExploreTopBar() }, bottomBar = { BuyerBottomBar(1) }) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SearchBox()
            Spacer(Modifier.height(8.dp))
            Text("Preview sin datos de API", color = Color(0xFF6B7280))
        }
    }
}
