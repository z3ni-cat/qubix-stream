package com.qubix.stream.provider

import com.qubix.stream.data.*
import org.jsoup.Jsoup

/**
 * Provider Drama Asia sub Indo (Korea, China, Jepang).
 */
class DramaProvider : BaseProvider(
    id = "drama-subindo",
    name = "Drama Sub Indo",
    baseUrl = ProviderConfig.dramaBaseUrl,
    supportedTypes = listOf(MediaType.DRAMA)
) {
    override suspend fun getTrending(): List<MediaItem> = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(baseUrl), baseUrl)
        doc.select(".drama-list .drama-item, .listupd .bs").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            MediaItem(
                id = a.attr("href"),
                title = el.selectFirst(".tt, h3")?.text() ?: "?",
                posterUrl = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } } ?: "",
                type = MediaType.DRAMA,
                detailUrl = absUrl(a.attr("href")),
                providerId = id
            )
        }.take(30)
    }.getOrDefault(emptyList())

    override suspend fun search(query: String): List<MediaItem> = runCatching {
        val url = "$baseUrl/?s=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val doc = Jsoup.parse(ProviderHttp.get(url), baseUrl)
        doc.select(".listupd .bs, .drama-list .drama-item").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            MediaItem(
                id = a.attr("href"),
                title = el.selectFirst(".tt, h3")?.text() ?: "?",
                posterUrl = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } } ?: "",
                type = MediaType.DRAMA,
                detailUrl = absUrl(a.attr("href")),
                providerId = id
            )
        }.take(30)
    }.getOrDefault(emptyList())

    override suspend fun getDetail(url: String): MediaDetail = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(url), baseUrl)
        val title = doc.selectFirst(".infox h1, h1.entry-title")?.text() ?: "?"
        val poster = doc.selectFirst(".thumb img")?.let {
            it.attr("src").ifBlank { it.attr("data-src") }
        } ?: ""
        val synopsis = doc.selectFirst(".sinopsis, .desc")?.text() ?: ""
        val episodes = doc.select(".eplister li a").mapIndexed { i, a ->
            Episode(a.attr("href"), a.selectFirst(".epl-title")?.text() ?: "Episode ${i + 1}",
                i + 1, absUrl(a.attr("href")))
        }.reversed()
        MediaDetail(MediaItem(url, title, poster, MediaType.DRAMA, detailUrl = url, providerId = id),
            synopsis, emptyList(), episodes)
    }.getOrDefault(MediaDetail(MediaItem(url, "?", "", MediaType.DRAMA, detailUrl = url, providerId = id), ""))

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(episodeUrl), baseUrl)
        doc.select("iframe").flatMap { iframe ->
            val src = iframe.attr("src").ifBlank { iframe.attr("data-src") }
            if (src.isBlank()) emptyList() else extractStream(absUrl(src), episodeUrl)
        }
    }.getOrDefault(emptyList())

    private fun extractStream(embedUrl: String, referer: String): List<VideoSource> = runCatching {
        val html = ProviderHttp.get(embedUrl, referer)
        val out = mutableListOf<VideoSource>()
        Regex("""(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)""").findAll(html).forEach {
            out += VideoSource(it.groupValues[1], "Auto", isM3u8 = true, mapOf("Referer" to embedUrl))
        }
        Regex("""(https?://[^\s"'<>]+\.mp4[^\s"'<>]*)""").findAll(html).take(3).forEach {
            out += VideoSource(it.groupValues[1], "HD", headers = mapOf("Referer" to embedUrl))
        }
        out.distinctBy { it.url }.take(5)
    }.getOrDefault(emptyList())
}
