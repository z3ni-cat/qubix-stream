package com.qubix.stream.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.qubix.stream.data.MediaItem
import com.qubix.stream.data.MediaType
import com.qubix.stream.provider.ProviderManager
import kotlinx.coroutines.launch

@Composable
fun MediaCard(item: MediaItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(120.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier.height(170.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(6.dp))
        Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        if (item.year.isNotBlank()) Text(item.year, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HomeScreen(onOpenDetail: (String, String) -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var sections by remember { mutableStateOf<Map<String, List<MediaItem>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Semua", "Film", "Anime", "Drama", "Serial")
    val typeFilter = listOf(null, MediaType.MOVIE, MediaType.ANIME, MediaType.DRAMA, MediaType.SERIES)

    LaunchedEffect(Unit) {
        scope.launch {
            sections = ProviderManager.trendingAll()
            loading = false
        }
    }

    Column(modifier.fillMaxSize()) {
        Text("Qubix Stream", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp))
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ProviderManager.active()
                    .filter { p -> typeFilter[tab] == null || p.supportedTypes.contains(typeFilter[tab]) }
                    .forEach { p ->
                        val items = sections[p.id].orEmpty()
                        if (items.isNotEmpty()) {
                            item {
                                Column {
                                    Text(p.name, style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(items) { m ->
                                            MediaCard(m, onClick = { onOpenDetail(m.detailUrl, m.providerId) })
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}
