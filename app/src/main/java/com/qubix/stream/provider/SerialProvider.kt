package com.qubix.stream.provider

import com.qubix.stream.data.*
import org.jsoup.Jsoup

/**
 * Provider Serial TV Barat sub Indo.
 */
class SerialProvider : BaseProvider(
    id = "serial-subindo",
    name = "Serial Sub Indo",
    baseUrl = ProviderConfig.serialBaseUrl,
    supportedTypes = listOf(MediaType.SERIES)
) {
    override suspend fun getTrending(): List<MediaItem> = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(baseUrl), baseUrl)
        doc.select(".series-list .series-item, .items .item").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            MediaItem(
                id = a.attr("href"),
                title = el.selectFirst(".title, h3")?.text() ?: "?",
                posterUrl = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } } ?: "",
                type = MediaType.SERIES,
                detailUrl = absUrl(a.attr("href")),
                providerId = id
            )
        }.take(30)
    }.getOrDefault(emptyList())

    override suspend fun search(query: String): List<MediaItem> = runCatching {
        val url = "$baseUrl/?s=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val doc = Jsoup.parse(ProviderHttp.get(url), baseUrl)
        doc.select(".items .item, .series-list .series-item").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            MediaItem(
                id = a.attr("href"),
                title = el.selectFirst(".title, h3")?.text() ?: "?",
                posterUrl = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } } ?: "",
                type = MediaType.SERIES,
                detailUrl = absUrl(a.attr("href")),
                providerId = id
            )
        }.take(30)
    }.getOrDefault(emptyList())

    override suspend fun getDetail(url: String): MediaDetail = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(url), baseUrl)
        val title = doc.selectFirst("h1.entry-title, .m-title")?.text() ?: "?"
        val poster = doc.selectFirst(".poster img")?.let {
            it.attr("src").ifBlank { it.attr("data-src") }
        } ?: ""
        val synopsis = doc.selectFirst(".synopsis, .desc")?.text() ?: ""
        // Kelompokkan per season jika ada
        val episodes = doc.select(".episode-list a, .eplister li a").mapIndexed { i, a ->
            Episode(a.attr("href"),
                a.text().ifBlank { "Episode ${i + 1}" }, i + 1, absUrl(a.attr("href")))
        }
        MediaDetail(MediaItem(url, title, poster, MediaType.SERIES, detailUrl = url, providerId = id),
            synopsis, emptyList(), episodes)
    }.getOrDefault(MediaDetail(MediaItem(url, "?", "", MediaType.SERIES, detailUrl = url, providerId = id), ""))

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(episodeUrl), baseUrl)
        val sources = mutableListOf<VideoSource>()
        doc.select("iframe").forEach { iframe ->
            val src = iframe.attr("src").ifBlank { iframe.attr("data-src") }
            if (src.isNotBlank()) sources += extractStream(absUrl(src), episodeUrl)
        }
        sources.distinctBy { it.url }.take(5)
    }.getOrDefault(emptyList())

    private fun extractStream(embedUrl: String, referer: String): List<VideoSource> = runCatching {
        val html = ProviderHttp.get(embedUrl, referer)
        val out = mutableListOf<VideoSource>()
        Regex("""sources\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(html)?.let { m ->
            Regex(""""file"\s*:\s*"([^"]+)"""").findAll(m.groupValues[1]).forEach { f ->
                val u = f.groupValues[1]
                val label = Regex(""""label"\s*:\s*"([^"]+)"""").find(m.groupValues[1])?.groupValues?.get(1) ?: "Auto"
                out += VideoSource(u, label, isM3u8 = u.contains("m3u8"), mapOf("Referer" to embedUrl))
            }
        }
        if (out.isEmpty()) {
            Regex("""(https?://[^\s"'<>]+\.(m3u8|mp4)[^\s"'<>]*)""").findAll(html).take(4).forEach {
                out += VideoSource(it.groupValues[1], "Auto",
                    isM3u8 = it.groupValues[2] == "m3u8", mapOf("Referer" to embedUrl))
            }
        }
        out.distinctBy { it.url }
    }.getOrDefault(emptyList())
}
