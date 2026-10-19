package com.qubix.stream.ui

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.qubix.stream.QubixStreamApp
import com.qubix.stream.data.VideoSource
import com.qubix.stream.db.HistoryEntity
import com.qubix.stream.player.StreamPlayer
import com.qubix.stream.provider.ProviderManager
import com.qubix.stream.ui.theme.QubixStreamTheme
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val episodeUrl = intent.getStringExtra("episodeUrl") ?: return finish()
        val episodeTitle = intent.getStringExtra("episodeTitle") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val posterUrl = intent.getStringExtra("posterUrl") ?: ""
        val detailUrl = intent.getStringExtra("detailUrl") ?: ""
        val providerId = intent.getStringExtra("providerId") ?: ""
        setContent {
            QubixStreamTheme {
                PlayerScreen(episodeUrl, episodeTitle, title, posterUrl, detailUrl, providerId,
                    onBack = { finish() })
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    episodeUrl: String, episodeTitle: String, title: String, posterUrl: String,
    detailUrl: String, providerId: String, onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val streamPlayer = remember { StreamPlayer(context) }
    var sources by remember { mutableStateOf<List<VideoSource>>(emptyList()) }
    var selected by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    val isLoading by streamPlayer.isLoading.collectAsState()
    val error by streamPlayer.error.collectAsState()

    DisposableEffect(Unit) { onDispose { streamPlayer.release() } }

    LaunchedEffect(episodeUrl) {
        scope.launch {
            val p = ProviderManager.byId(providerId)
            val list = p?.getVideoSources(episodeUrl).orEmpty()
            sources = list
            loading = false
            list.firstOrNull()?.let { streamPlayer.playSource(it) }
            // Simpan riwayat
            QubixStreamApp.instance.db.history().upsert(
                HistoryEntity(episodeUrl, title, episodeTitle, posterUrl, detailUrl, providerId))
        }
    }

    // Update posisi tontonan berkala
    LaunchedEffect(sources) {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(10_000)
                val pos = streamPlayer.player.currentPosition
                val dur = streamPlayer.player.duration.coerceAtLeast(0)
                if (dur > 0) QubixStreamApp.instance.db.history().upsert(
                    HistoryEntity(episodeUrl, title, episodeTitle, posterUrl, detailUrl,
                        providerId, pos, dur))
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Kembali") }
            Text(episodeTitle, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
        }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = streamPlayer.player
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )
        when {
            loading || isLoading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> Text("Gagal memuat: $error",
                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        }
        if (sources.size > 1) {
            Text("Server:", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            sources.forEachIndexed { i, s ->
                val sel = i == selected
                FilterChip(
                    selected = sel,
                    onClick = { selected = i; streamPlayer.playSource(s) },
                    label = { Text("${s.quality} • Server ${i + 1}") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Subtitle Indonesia otomatis aktif jika tersedia dari provider.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}
