package com.qubix.stream.provider

import com.qubix.stream.data.Episode
import com.qubix.stream.data.MediaDetail
import com.qubix.stream.data.MediaItem
import com.qubix.stream.data.VideoSource

/**
 * Interface yang harus diimplementasikan setiap provider.
 * Provider = sumber konten (scraper situs streaming sub Indo).
 */
interface Provider {
    val id: String
    val name: String
    val baseUrl: String
    val supportedTypes: List<com.qubix.stream.data.MediaType>
    var enabled: Boolean

    suspend fun getTrending(): List<MediaItem>
    suspend fun search(query: String): List<MediaItem>
    suspend fun getDetail(url: String): MediaDetail
    suspend fun getVideoSources(episodeUrl: String): List<VideoSource>
}

abstract class BaseProvider(
    override val id: String,
    override val name: String,
    override var baseUrl: String,
    override val supportedTypes: List<com.qubix.stream.data.MediaType>,
    override var enabled: Boolean = true
) : Provider {
    protected val httpClient by lazy { ProviderHttp.client }

    protected fun absUrl(path: String): String =
        if (path.startsWith("http")) path else baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
