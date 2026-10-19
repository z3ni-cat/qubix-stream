package com.qubix.stream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.qubix.stream.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { QubixStreamRoot() }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun QubixStreamRoot() {
    com.qubix.stream.ui.theme.QubixStreamTheme {
        val tabs = listOf(
            Tab("home", "Beranda", Icons.Filled.Home),
            Tab("search", "Cari", Icons.Filled.Search),
            Tab("library", "Saya", Icons.Filled.Bookmarks)
        )
        var current by remember { mutableStateOf("home") }
        var detailUrl by remember { mutableStateOf<String?>(null) }
        var detailProvider by remember { mutableStateOf<String?>(null) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = current == t.route && detailUrl == null,
                            onClick = { current = t.route; detailUrl = null },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        ) { pad ->
            when {
                detailUrl != null -> DetailScreen(
                    detailUrl = detailUrl!!,
                    providerId = detailProvider!!,
                    onBack = { detailUrl = null },
                    modifier = Modifier.padding(pad)
                )
                current == "home" -> HomeScreen(
                    onOpenDetail = { url, pid -> detailUrl = url; detailProvider = pid },
                    modifier = Modifier.padding(pad)
                )
                current == "search" -> SearchScreen(
                    onOpenDetail = { url, pid -> detailUrl = url; detailProvider = pid },
                    modifier = Modifier.padding(pad)
                )
                else -> LibraryScreen(
                    onOpenDetail = { url, pid -> detailUrl = url; detailProvider = pid },
                    modifier = Modifier.padding(pad)
                )
            }
        }
    }
}
