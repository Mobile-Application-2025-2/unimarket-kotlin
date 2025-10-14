package com.example.unimarket.view.scaffold

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

private val Accent = Color(0xFFF7B500)

sealed class BottomDest(val route: String, val icon: ImageVector, val label: String) {
    object Home: BottomDest("home", Icons.Outlined.Map, "Map")
    object Orders: BottomDest("orders", Icons.Outlined.ShoppingCart, "Orders")
    object Chat: BottomDest("chat", Icons.Outlined.Chat, "Chat")
    object Profile: BottomDest("profile", Icons.Outlined.Person, "Profile")
}
private val bottomItems = listOf(BottomDest.Home, BottomDest.Orders, BottomDest.Chat, BottomDest.Profile)

@Composable
fun AppBottomBar(current: BottomDest, onNavigate: (BottomDest) -> Unit) {
    NavigationBar(containerColor = Accent) {
        bottomItems.forEach { dest ->
            NavigationBarItem(
                selected = current::class == dest::class,
                onClick = { onNavigate(dest) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = null,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.75f),
                    indicatorColor = Color.White.copy(alpha = 0.18f)
                )
            )
        }
    }
}

@Composable
fun AppScaffold(
    current: BottomDest,
    onNavigate: (BottomDest) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = { AppBottomBar(current, onNavigate) },
        containerColor = Color.White
    ) { padding -> content(padding) }
}
