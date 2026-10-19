package com.qubix.stream.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qubix.stream.data.MediaItem
import com.qubix.stream.provider.ProviderManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onOpenDetail: (String, String) -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                job?.cancel()
                if (it.length >= 3) {
                    job = scope.launch {
                        delay(600)
                        searching = true
                        results = ProviderManager.searchAll(it)
                        searching = false
                    }
                } else results = emptyList()
            },
            label = { Text("Cari film, anime, drama...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        if (searching) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (results.isEmpty() && query.length >= 3) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ketemu. Coba kata kunci lain.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results) { m ->
                    MediaCard(m, onClick = { onOpenDetail(m.detailUrl, m.providerId) },
                        modifier = Modifier)
                }
            }
        }
    }
}
