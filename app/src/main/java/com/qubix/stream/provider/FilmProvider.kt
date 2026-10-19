package com.qubix.stream.provider

import com.qubix.stream.data.*
import org.jsoup.Jsoup

/**
 * Provider Film sub Indo.
 * Pola: situs film WordPress (ala IDLIX/DutaFilm).
 */
class FilmProvider : BaseProvider(
    id = "film-subindo",
    name = "Film Sub Indo",
    baseUrl = ProviderConfig.filmBaseUrl,
    supportedTypes = listOf(MediaType.MOVIE)
) {
    override suspend fun getTrending(): List<MediaItem> = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(baseUrl), baseUrl)
        doc.select(".movie-list .movie-item, .items .item").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            MediaItem(
                id = a.attr("href"),
                title = el.selectFirst(".title, h3")?.text() ?: a.attr("title"),
                posterUrl = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } } ?: "",
                type = MediaType.MOVIE,
                year = el.selectFirst(".year")?.text() ?: "",
                rating = el.selectFirst(".rating")?.text() ?: "",
                detailUrl = absUrl(a.attr("href")),
                providerId = id
            )
        }.take(30)
    }.getOrDefault(emptyList())

    override suspend fun search(query: String): List<MediaItem> = runCatching {
        val url = "$baseUrl/?s=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val doc = Jsoup.parse(ProviderHttp.get(url), baseUrl)
        doc.select(".items .item, .movie-list .movie-item").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            MediaItem(
                id = a.attr("href"),
                title = el.selectFirst(".title, h3")?.text() ?: "?",
                posterUrl = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } } ?: "",
                type = MediaType.MOVIE,
                detailUrl = absUrl(a.attr("href")),
                providerId = id
            )
        }.take(30)
    }.getOrDefault(emptyList())

    override suspend fun getDetail(url: String): MediaDetail = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(url), baseUrl)
        val title = doc.selectFirst("h1.entry-title, .m-title")?.text() ?: "?"
        val poster = doc.selectFirst(".poster img, .thumb img")?.let {
            it.attr("src").ifBlank { it.attr("data-src") }
        } ?: ""
        val synopsis = doc.selectFirst(".synopsis, .desc p")?.text() ?: ""
        // Film = 1 episode (play langsung)
        val ep = Episode(id = url, title = title, number = 1, url = url)
        MediaDetail(
            MediaItem(url, title, poster, MediaType.MOVIE, detailUrl = url, providerId = id),
            synopsis, emptyList(), listOf(ep)
        )
    }.getOrDefault(MediaDetail(MediaItem(url, "?", "", MediaType.MOVIE, detailUrl = url, providerId = id), ""))

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(episodeUrl), baseUrl)
        val sources = mutableListOf<VideoSource>()
        // Server list: tombol pilih server -> iframe
        doc.select(".server-list button, .servers li").forEach { btn ->
            val dataSrc = btn.attr("data-src").ifBlank { btn.attr("data-embed") }
            if (dataSrc.isNotBlank()) {
                sources += extractStream(absUrl(dataSrc), episodeUrl)
            }
        }
        // Fallback: iframe langsung
        if (sources.isEmpty()) {
            doc.select("iframe").forEach { iframe ->
                val src = iframe.attr("src").ifBlank { iframe.attr("data-src") }
                if (src.isNotBlank()) sources += extractStream(absUrl(src), episodeUrl)
            }
        }
        // Subtitle dari halaman (track kind=subtitles)
        val subs = doc.select("track[kind=subtitles], .sub-list a").mapNotNull { t ->
            val src = t.attr("src").ifBlank { t.attr("href") }
            if (src.isBlank()) null else Subtitle(absUrl(src), "id", t.attr("label").ifBlank { "Indonesia" })
        }
        sources.map { it.copy(subtitles = it.subtitles + subs) }
    }.getOrDefault(emptyList())

    private fun extractStream(embedUrl: String, referer: String): List<VideoSource> = runCatching {
        val html = ProviderHttp.get(embedUrl, referer)
        val out = mutableListOf<VideoSource>()
        Regex(""""file"\s*:\s*"([^"]+)"""").findAll(html).forEach { m ->
            val u = m.groupValues[1]
            out += VideoSource(u, if (u.contains("m3u8")) "Auto" else "HD",
                isM3u8 = u.contains("m3u8"), headers = mapOf("Referer" to embedUrl))
        }
        Regex("""<source[^>]+src="([^"]+)"""").findAll(html).forEach { m ->
            val u = m.groupValues[1]
            out += VideoSource(u, "HD", headers = mapOf("Referer" to embedUrl))
        }
        out.distinctBy { it.url }.take(5)
    }.getOrDefault(emptyList())
}
