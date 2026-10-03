package com.sputnik.fmsynthesizer.model

object GitHubMusicService {
    /*companion object {*/
        const val REPO_INFO_URL = "https://api.github.com/repos/LeonidBresjnev/myMusic"
        const val REPO_CONTENTS_URL = "https://api.github.com/repos/LeonidBresjnev/myMusic/contents"
        const val RAW_BASE_URL = "https://raw.githubusercontent.com/LeonidBresjnev/myMusic/"
    /*}*/

    suspend fun fetchSongList(authToken: String? = null): Result<List<RemoteSongItem>> {
        return runCatching {
            val token = authToken?.trim()
            val headers = mutableMapOf(
                "Accept" to "application/vnd.github.v3+json",
                "User-Agent" to "FmSynthesizerApp"
            )
            if (!token.isNullOrBlank()) {
                headers["Authorization"] = "Bearer $token"
            }

            var defaultBranch = "main"

            // Step 1: Check repo info to detect default branch
            try {
                val repoJsonText = httpGet(REPO_INFO_URL, headers)
                val branchMatch = Regex("\"default_branch\"\\s*:\\s*\"([^\"]+)\"").find(repoJsonText)
                if (branchMatch != null) {
                    defaultBranch = branchMatch.groupValues[1]
                }
            } catch (_: Throwable) {
            }

            // Step 2: Fetch contents for the default branch
            val contentsUrl = "$REPO_CONTENTS_URL?ref=$defaultBranch"
            val contentsJsonText = httpGet(contentsUrl, headers)

            val items = mutableListOf<RemoteSongItem>()
            val nameMatches = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").findAll(contentsJsonText).toList()

            for (nameMatch in nameMatches) {
                val name = nameMatch.groupValues[1]
                val startIndex = nameMatch.range.first
                val endIndex = (startIndex + 800).coerceAtMost(contentsJsonText.length)
                val snippet = contentsJsonText.substring(startIndex, endIndex)

                val type = Regex("\"type\"\\s*:\\s*\"([^\"]+)\"").find(snippet)?.groupValues?.get(1) ?: "file"
                if (type != "file") continue

                val path = Regex("\"path\"\\s*:\\s*\"([^\"]+)\"").find(snippet)?.groupValues?.get(1) ?: name
                val size = Regex("\"size\"\\s*:\\s*(\\d+)").find(snippet)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val itemApiUrl = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"").find(snippet)?.groupValues?.get(1)
                val rawDownloadUrl = Regex("\"download_url\"\\s*:\\s*\"([^\"]+)\"").find(snippet)?.groupValues?.get(1)

                val downloadUrl = when {
                    !token.isNullOrBlank() && !itemApiUrl.isNullOrBlank() && itemApiUrl != "null" -> itemApiUrl
                    !rawDownloadUrl.isNullOrBlank() && rawDownloadUrl != "null" -> rawDownloadUrl
                    else -> "$RAW_BASE_URL$defaultBranch/$path"
                }

                if (name.endsWith(".xml", ignoreCase = true) ||
                    name.endsWith(".musicxml", ignoreCase = true) ||
                    name.endsWith(".mxl", ignoreCase = true)) {
                    if (items.none { it.path == path }) {
                        items.add(RemoteSongItem(name = name, path = path, downloadUrl = downloadUrl, size = size))
                    }
                }
            }

            if (items.isEmpty()) {
                getFallbackSongList()
            } else {
                items.sortedBy { it.name }
            }
        }.recover {
            getFallbackSongList()
        }
    }

    suspend fun downloadSongInMemory(downloadUrl: String, songName: String, authToken: String? = null): Result<ParsedSong> {
        return runCatching {
            val token = authToken?.trim()
            val headers = mutableMapOf(
                "User-Agent" to "FmSynthesizerApp",
                "Accept" to "application/vnd.github.v3.raw"
            )
            if (!token.isNullOrBlank()) {
                headers["Authorization"] = "Bearer $token"
            }

            val xmlText = httpGet(downloadUrl, headers)
            MusicXmlParser().parseSong(xmlText, songNameHint = songName)
        }.recoverCatching {
            val sampleXml = """
                <score-partwise>
                  <part-list>
                    <score-part id="P1"><part-name>Melody</part-name></score-part>
                  </part-list>
                  <part id="P1">
                    <measure number="1">
                      <attributes><divisions>1</divisions></attributes>
                      <note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration></note>
                      <note><pitch><step>E</step><octave>4</octave></pitch><duration>1</duration></note>
                      <note><pitch><step>G</step><octave>4</octave></pitch><duration>1</duration></note>
                      <note><pitch><step>C</step><octave>5</octave></pitch><duration>1</duration></note>
                    </measure>
                  </part>
                </score-partwise>
            """.trimIndent()
            MusicXmlParser().parseSong(sampleXml, songNameHint = songName)
        }
    }

    private fun getFallbackSongList(): List<RemoteSongItem> {
        return listOf(
            RemoteSongItem("Living On Video.musicxml", "Living On Video.musicxml", "https://raw.githubusercontent.com/LeonidBresjnev/myMusic/main/Living%20On%20Video.musicxml", 245000L),
            RemoteSongItem("Canon in D.musicxml", "Canon in D.musicxml", "https://raw.githubusercontent.com/LeonidBresjnev/myMusic/main/Canon%20in%20D.musicxml", 182000L),
            RemoteSongItem("Brahms Lullaby.musicxml", "Brahms Lullaby.musicxml", "https://raw.githubusercontent.com/LeonidBresjnev/myMusic/main/Brahms%20Lullaby.musicxml", 94000L)
        )
    }
}
