package com.example.unimarket.view.explore

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
import androidx.compose.view.Alignment
import androidx.compose.view.Modifier
import androidx.compose.view.draw.clip
import androidx.compose.view.graphics.Color
import androidx.compose.view.graphics.vector.ImageVector
import androidx.compose.view.layout.ContentScale
import androidx.compose.view.res.painterResource
import androidx.compose.view.text.font.FontWeight
import androidx.compose.view.text.style.TextOverflow
import androidx.compose.view.tooling.preview.Preview
import androidx.compose.view.unit.dp
import androidx.compose.view.unit.sp
import com.example.unimarket.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class ExploreBuyerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExploreBuyerScreen() }
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

/* ------------------------- DATA ------------------------- */

private data class ExploreItem(
    val title: String,
    @DrawableRes val imageRes: Int,
    val bg: Color,
    val highlighted: Boolean = false
)

private val demoItems = listOf(
    ExploreItem("Tutorías IIND-2106", R.drawable.tutoriasiind2106, Pastels[0]),
    ExploreItem("Tutorías ISIS-1221", R.drawable.tutoriasisis1221, Pastels[1]),
    ExploreItem("Venta de brownies", R.drawable.brownies, Pastels[3]),
    ExploreItem("Venta de funkos",   R.drawable.funko,    Pastels[2]),
    ExploreItem("Comida mexicana",   R.drawable.tacos,    Pastels[5], highlighted = true),
    ExploreItem("Comida italiana",   R.drawable.pasta,    Pastels[4])
)

private data class CatChip(val label: String, val icon: ImageVector? = null)
private val catChips = listOf(
    CatChip("Todos", Icons.Outlined.AllInbox),
    CatChip("Comida", Icons.Outlined.Fastfood),
    CatChip("Papelería", Icons.Outlined.Edit),
    CatChip("Tecnología", Icons.Outlined.Memory),
    CatChip("Tutorías", Icons.Outlined.School),
    CatChip("Ropa", Icons.Outlined.Checkroom),
    CatChip("Arte", Icons.Outlined.Brush),
    CatChip("Deporte", Icons.Outlined.SportsBasketball)
)

/* ------------------------- SCREEN ------------------------- */

@Composable
fun ExploreBuyerScreen() {
    var selectedChip by remember { mutableStateOf(0) }

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

            // 🔹 CHIPS EN SCROLL HORIZONTAL
            CategoryChipsRow(
                chips = catChips,
                selectedIndex = selectedChip,
                onSelect = { selectedChip = it }
            )

            Spacer(Modifier.height(8.dp))

            val rows = remember(demoItems) { demoItems.chunked(2) }
            rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { item ->
                        ExploreCard(
                            item = item,
                            modifier = Modifier
                                .weight(1f)
                                .height(150.dp)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(72.dp)) // separador por bottom bar
        }
    }
}

/* ------------------------- SUBCOMPONENTES ------------------------- */

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

/** Chips en scroll horizontal (LazyRow) */
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
private fun ExploreCard(item: ExploreItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
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
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.9f)),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(12.dp))
            Text(
                item.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ------------------------- BOTTOM BAR ------------------------- */

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

/* ------------------------- PREVIEW ------------------------- */

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_6")
@Composable
private fun PreviewExploreBuyer() {
    ExploreBuyerScreen()
}
