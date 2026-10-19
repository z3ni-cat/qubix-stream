package com.qubix.stream.provider

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * HTTP client bersama untuk semua provider.
 * User-Agent browser agar tidak diblokir proteksi dasar.
 */
object ProviderHttp {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun get(url: String, referer: String? = null): String {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36")
            .header("Accept-Language", "id-ID,id;q=0.9,en;q=0.8")
        if (referer != null) builder.header("Referer", referer)
        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            return resp.body?.string() ?: ""
        }
    }

    class IOException(msg: String) : Exception(msg)
}

/**
 * Konfigurasi base URL tiap provider — bisa diubah user dari Settings
 * tanpa perlu update aplikasi (antisipasi domain berganti).
 */
object ProviderConfig {
    var animeBaseUrl: String = "https://example-anime-subindo.site"
    var filmBaseUrl: String = "https://example-film-subindo.site"
    var dramaBaseUrl: String = "https://example-drama-subindo.site"
    var serialBaseUrl: String = "https://example-serial-subindo.site"
}
