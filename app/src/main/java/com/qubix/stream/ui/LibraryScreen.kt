package com.qubix.stream.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.qubix.stream.QubixStreamApp
import com.qubix.stream.db.BookmarkEntity
import com.qubix.stream.db.HistoryEntity
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(onOpenDetail: (String, String) -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var bookmarks by remember { mutableStateOf<List<BookmarkEntity>>(emptyList()) }
    var history by remember { mutableStateOf<List<HistoryEntity>>(emptyList()) }
    val db = QubixStreamApp.instance.db

    LaunchedEffect(Unit) {
        scope.launch {
            bookmarks = db.bookmarks().all()
            history = db.history().recent()
        }
    }

    Column(modifier.fillMaxSize()) {
        Text("Koleksi Saya", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Bookmark") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Riwayat") })
        }
        if (tab == 0) {
            if (bookmarks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Belum ada bookmark.")
                }
            } else LazyColumn(contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(bookmarks) { b ->
                    Row(Modifier.fillMaxWidth().clickable { onOpenDetail(b.detailUrl, b.providerId) },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(b.posterUrl, b.title,
                            modifier = Modifier.width(64.dp).height(90.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop)
                        Column {
                            Text(b.title, fontWeight = FontWeight.Medium)
                            Text(b.type, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            if (history.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Belum ada riwayat tontonan.")
                }
            } else LazyColumn(contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history) { h ->
                    val progress = if (h.durationMs > 0) h.positionMs.toFloat() / h.durationMs else 0f
                    Column(Modifier.fillMaxWidth().clickable { onOpenDetail(h.detailUrl, h.providerId) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AsyncImage(h.posterUrl, h.title,
                                modifier = Modifier.width(64.dp).height(90.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop)
                            Column {
                                Text(h.title, fontWeight = FontWeight.Medium)
                                Text(h.episodeTitle, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (progress > 0) LinearProgressIndicator(
                            progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
