package com.qubix.stream.data

enum class MediaType { MOVIE, ANIME, DRAMA, SERIES }

data class MediaItem(
    val id: String,
    val title: String,
    val posterUrl: String,
    val type: MediaType,
    val year: String = "",
    val rating: String = "",
    val detailUrl: String,
    val providerId: String
)

data class MediaDetail(
    val item: MediaItem,
    val synopsis: String,
    val genres: List<String> = emptyList(),
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val id: String,
    val title: String,
    val number: Int,
    val url: String,
    val thumbnailUrl: String = ""
)

data class VideoSource(
    val url: String,
    val quality: String = "Auto",
    val isM3u8: Boolean = false,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<Subtitle> = emptyList()
)

data class Subtitle(
    val url: String,
    val language: String,
    val label: String
)
