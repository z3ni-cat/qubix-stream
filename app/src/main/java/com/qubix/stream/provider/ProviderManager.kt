package com.qubix.stream.provider

import com.qubix.stream.data.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * Mengelola 4 provider sub Indo yang pre-installed & langsung aktif.
 * Provider tambahan bisa didaftarkan di sini di versi berikutnya.
 */
object ProviderManager {
    private val providers: List<Provider> = listOf(
        AnimeProvider(),
        FilmProvider(),
        DramaProvider(),
        SerialProvider()
    )

    fun all(): List<Provider> = providers
    fun active(): List<Provider> = providers.filter { it.enabled }
    fun byId(id: String): Provider? = providers.find { it.id == id }
    fun byType(type: MediaType): List<Provider> =
        active().filter { it.supportedTypes.contains(type) }

    suspend fun trendingAll(): Map<String, List<com.qubix.stream.data.MediaItem>> =
        withContext(Dispatchers.IO) {
            active().map { p -> async { p.id to p.getTrending() } }
                .awaitAll().toMap()
        }

    suspend fun searchAll(query: String): List<com.qubix.stream.data.MediaItem> =
        withContext(Dispatchers.IO) {
            active().map { p -> async { p.search(query) } }
                .awaitAll().flatten()
        }

    /** Refresh base URL saat user mengubahnya dari Settings. */
    fun updateBaseUrls() {
        (byId("anime-subindo") as? AnimeProvider)?.baseUrl = ProviderConfig.animeBaseUrl
        (byId("film-subindo") as? FilmProvider)?.baseUrl = ProviderConfig.filmBaseUrl
        (byId("drama-subindo") as? DramaProvider)?.baseUrl = ProviderConfig.dramaBaseUrl
        (byId("serial-subindo") as? SerialProvider)?.baseUrl = ProviderConfig.serialBaseUrl
    }
}
