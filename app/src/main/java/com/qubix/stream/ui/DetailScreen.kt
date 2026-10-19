package com.qubix.stream.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.qubix.stream.QubixStreamApp
import com.qubix.stream.data.MediaDetail
import com.qubix.stream.db.BookmarkEntity
import com.qubix.stream.provider.ProviderManager
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(detailUrl: String, providerId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var detail by remember { mutableStateOf<MediaDetail?>(null) }
    var bookmarked by remember { mutableStateOf(false) }
    val db = QubixStreamApp.instance.db

    LaunchedEffect(detailUrl) {
        scope.launch {
            val p = ProviderManager.byId(providerId) ?: return@launch
            detail = p.getDetail(detailUrl)
            bookmarked = db.bookmarks().exists(detailUrl)
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Kembali") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                scope.launch {
                    detail?.let { d ->
                        if (bookmarked) db.bookmarks().delete(detailUrl)
                        else db.bookmarks().insert(
                            BookmarkEntity(detailUrl, d.item.title, d.item.posterUrl,
                                d.item.type.name, providerId))
                        bookmarked = !bookmarked
                    }
                }
            }) { Icon(if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Bookmark") }
        }
        detail?.let { d ->
            LazyColumn(contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AsyncImage(d.item.posterUrl, d.item.title,
                            modifier = Modifier.width(120.dp).height(170.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop)
                        Column {
                            Text(d.item.title, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            if (d.genres.isNotEmpty())
                                Text(d.genres.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            ProviderManager.byId(providerId)?.let {
                                AssistChip(onClick = {}, label = { Text(it.name) })
                            }
                        }
                    }
                }
                if (d.synopsis.isNotBlank()) item {
                    Text(d.synopsis, style = MaterialTheme.typography.bodyMedium)
                }
                item {
                    Text(if (d.episodes.size > 1) "Episode (${d.episodes.size})" else "Putar",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(d.episodes) { ep ->
                    ListItem(
                        headlineContent = { Text(ep.title) },
                        leadingContent = { Icon(Icons.Filled.PlayArrow, null) },
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                                putExtra("episodeUrl", ep.url)
                                putExtra("episodeTitle", ep.title)
                                putExtra("title", d.item.title)
                                putExtra("posterUrl", d.item.posterUrl)
                                putExtra("detailUrl", detailUrl)
                                putExtra("providerId", providerId)
                            })
                        }
                    )
                    HorizontalDivider()
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
