package com.example.unimarket.view.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import com.example.unimarket.SupaConst
import com.example.unimarket.model.api.AuthApiFactory
import com.example.unimarket.model.api.CategoriesApi
import com.example.unimarket.model.entity.Category
import com.example.unimarket.model.repository.ExploreRepository

@Composable
private fun rememberExploreRepository(): ExploreRepository {
    return remember {
        val retrofitRest = AuthApiFactory.buildRestRetrofit(
            baseUrl = SupaConst.SUPABASE_URL,
            anonKey = SupaConst.SUPABASE_ANON_KEY,
            userJwt = null,
            enableLogging = true
        )
        val api = retrofitRest.create(CategoriesApi::class.java)
        ExploreRepository(api)
    }
}

@Composable
fun ExploreCategoriesSection(
    repo: ExploreRepository = rememberExploreRepository(),
    onOpen: (Category) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<Category>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repo.getCategories()
            .onSuccess { items = it; error = null }
            .onFailure { error = it.message }
        loading = false
    }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Text(
            text = error ?: "Error",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { cat ->
                CategoryRow(
                    category = cat,
                    onClick = {
                        val old = cat
                        val idx = items.indexOfFirst { it.id == cat.id }
                        if (idx != -1) {
                            val bumped = old.copy(selectionCount = old.selectionCount + 1L)
                            items = items.toMutableList().also { it[idx] = bumped }

                            scope.launch {
                                repo.incrementCategorySelection(
                                    categoryId = cat.id,
                                    newCount = bumped.selectionCount
                                )
                                    .onSuccess { server ->
                                        val i = items.indexOfFirst { it.id == server.id }
                                        if (i != -1) {
                                            items = items.toMutableList().also { it[i] = server }
                                        }
                                        onOpen(server)
                                    }
                                    .onFailure {
                                        val i = items.indexOfFirst { it.id == old.id }
                                        if (i != -1) {
                                            items = items.toMutableList().also { it[i] = old }
                                        }
                                    }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onClick: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.Bold)
                Text(category.type, style = MaterialTheme.typography.bodySmall)
            }
            Text("${category.selectionCount}", fontWeight = FontWeight.SemiBold)
        }
    }
}