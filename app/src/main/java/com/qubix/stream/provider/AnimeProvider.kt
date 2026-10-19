package com.qubix.stream.provider

import com.qubix.stream.data.*
import org.jsoup.Jsoup

/**
 * Provider Anime sub Indo.
 * Pola: situs anime WordPress (ala Samehadaku/Otakudesu).
 * Base URL bisa diganti dari Settings.
 */
class AnimeProvider : BaseProvider(
    id = "anime-subindo",
    name = "Anime Sub Indo",
    baseUrl = ProviderConfig.animeBaseUrl,
    supportedTypes = listOf(MediaType.ANIME)
) {
    override suspend fun getTrending(): List<MediaItem> = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(baseUrl), baseUrl)
        doc.select(".anime-list .anime-item, .listupd .bs").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val img = el.selectFirst("img")
            MediaItem(
                id = a.attr("href"),
                title = a.attr("title").ifBlank { el.selectFirst(".tt, h2")?.text() ?: "?" },
                posterUrl = img?.attr("src") ?: img?.attr("data-src") ?: "",
                type = MediaType.ANIME,
                detailUrl = absUrl(a.attr("href")),
                providerId = id
            )
        }.take(30)
    }.getOrDefault(emptyList())

    override suspend fun search(query: String): List<MediaItem> = runCatching {
        val url = "$baseUrl/?s=${java.net.URLEncoder.encode(query, "UTF-8")}&post_type=anime"
        val doc = Jsoup.parse(ProviderHttp.get(url), baseUrl)
        doc.select(".listupd .bs, .anime-list .anime-item").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            MediaItem(
                id = a.attr("href"),
                title = el.selectFirst(".tt, h2")?.text() ?: a.attr("title"),
                posterUrl = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } } ?: "",
                type = MediaType.ANIME,
                detailUrl = absUrl(a.attr("href")),
                providerId = id
            )
        }.take(30)
    }.getOrDefault(emptyList())

    override suspend fun getDetail(url: String): MediaDetail = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(url), baseUrl)
        val title = doc.selectFirst(".infox h1, .entry-title")?.text() ?: "?"
        val poster = doc.selectFirst(".thumb img, .poster img")?.let {
            it.attr("src").ifBlank { it.attr("data-src") }
        } ?: ""
        val synopsis = doc.selectFirst(".sinopsis, .desc, .entry-content p")?.text() ?: ""
        val genres = doc.select(".genre-info a, .genres a").map { it.text() }
        val episodes = doc.select(".eplister li a, .episode-list a").mapIndexed { i, a ->
            Episode(
                id = a.attr("href"),
                title = a.selectFirst(".epl-title")?.text() ?: "Episode ${i + 1}",
                number = i + 1,
                url = absUrl(a.attr("href"))
            )
        }.reversed()
        MediaDetail(
            MediaItem(url, title, poster, MediaType.ANIME, detailUrl = url, providerId = id),
            synopsis, genres, episodes
        )
    }.getOrDefault(MediaDetail(MediaItem(url, "?", "", MediaType.ANIME, detailUrl = url, providerId = id), ""))

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> = runCatching {
        val doc = Jsoup.parse(ProviderHttp.get(episodeUrl), baseUrl)
        // Cari iframe embed server video
        doc.select("iframe").mapNotNull { iframe ->
            val src = iframe.attr("src").ifBlank { iframe.attr("data-src") }
            if (src.isBlank()) return@mapNotNull null
            extractStream(absUrl(src), episodeUrl)
        }.flatten()
    }.getOrDefault(emptyList())

    private fun extractStream(embedUrl: String, referer: String): List<VideoSource> = runCatching {
        val html = ProviderHttp.get(embedUrl, referer)
        // Pola umum: file:"url" atau source src="url.m3u8"
        val m3u8 = Regex("""file\s*:\s*"([^"]+\.m3u8[^"]*)"""").find(html)?.groupValues?.get(1)
            ?: Regex("""src\s*=\s*"([^"]+\.m3u8[^"]*)"""").find(html)?.groupValues?.get(1)
        val mp4 = Regex("""file\s*:\s*"([^"]+\.mp4[^"]*)"""").find(html)?.groupValues?.get(1)
        listOfNotNull(
            m3u8?.let { VideoSource(it, "Auto", isM3u8 = true, mapOf("Referer" to embedUrl)) },
            mp4?.let { VideoSource(it, "HD", headers = mapOf("Referer" to embedUrl)) }
        )
    }.getOrDefault(emptyList())
}
