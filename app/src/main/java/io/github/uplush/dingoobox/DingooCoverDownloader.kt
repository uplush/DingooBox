package io.github.uplush.dingoobox

import android.util.Log
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale

internal data class CoverDownloadSummary(
    val downloadedCount: Int,
    val existingCount: Int,
    val notFoundCount: Int,
    val failedCount: Int
)

/**
 * Downloads the screenshots maintained by the DingooEmu compatibility suite.
 *
 * MiNiQ can use Libretro's Pokemon Mini catalog because its ROM format has a
 * stable No-Intro identity. Dingoo .app files do not have an equivalent public
 * catalog, so this downloader deliberately uses a strict, curated mapping to
 * DingooEmu's own verified-game screenshots. A loose cross-platform title
 * search would install incorrect covers for common names such as Tetris.
 */
internal class DingooCoverDownloader {
    fun downloadMissingCovers(
        games: List<GameEntry>,
        installCover: (game: GameEntry, imageData: ByteArray) -> Boolean
    ): CoverDownloadSummary {
        var downloadedCount = 0
        var existingCount = 0
        var notFoundCount = 0
        var failedCount = 0

        games.forEach { game ->
            if (game.coverFile?.isFile == true) {
                existingCount += 1
                return@forEach
            }

            val catalogEntry = DingooCoverCatalog.find(game)
            if (catalogEntry == null) {
                notFoundCount += 1
                Log.i(LOG_TAG, "No curated cover match for ${game.fileName}")
                return@forEach
            }

            when (val attempt = downloadImage(catalogEntry.imageName)) {
                is DownloadAttempt.Success -> {
                    if (installCover(game, attempt.imageData)) {
                        downloadedCount += 1
                        Log.i(
                            LOG_TAG,
                            "Cover installed: ${game.fileName} -> ${catalogEntry.imageName}"
                        )
                    } else {
                        failedCount += 1
                    }
                }

                DownloadAttempt.NotFound -> {
                    notFoundCount += 1
                }

                DownloadAttempt.Failed -> {
                    failedCount += 1
                }
            }
        }

        return CoverDownloadSummary(
            downloadedCount = downloadedCount,
            existingCount = existingCount,
            notFoundCount = notFoundCount,
            failedCount = failedCount
        )
    }

    private fun downloadImage(imageName: String): DownloadAttempt {
        val encodedName = URLEncoder
            .encode(imageName, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        var connection: HttpURLConnection? = null

        return try {
            connection = (URL("$COVER_BASE_URL$encodedName").openConnection() as HttpURLConnection)
                .apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = true
                    useCaches = false
                    setRequestProperty("Accept", "image/png,image/*;q=0.8")
                    setRequestProperty("User-Agent", USER_AGENT)
                }

            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val announcedLength = connection.contentLengthLong
                    if (announcedLength > MAX_IMAGE_BYTES) {
                        DownloadAttempt.Failed
                    } else {
                        val imageData = connection.inputStream.buffered().use { input ->
                            readLimitedBytes(input, MAX_IMAGE_BYTES)
                        }
                        if (imageData == null || imageData.isEmpty()) {
                            DownloadAttempt.Failed
                        } else {
                            DownloadAttempt.Success(imageData)
                        }
                    }
                }

                HttpURLConnection.HTTP_NOT_FOUND -> DownloadAttempt.NotFound
                else -> DownloadAttempt.Failed
            }
        } catch (error: Exception) {
            Log.w(LOG_TAG, "Cover download failed for $imageName", error)
            DownloadAttempt.Failed
        } finally {
            connection?.disconnect()
        }
    }

    private fun readLimitedBytes(
        input: java.io.InputStream,
        maximumBytes: Long
    ): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        var total = 0L

        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            total += count
            if (total > maximumBytes) return null
            output.write(buffer, 0, count)
        }

        return output.toByteArray()
    }

    private sealed interface DownloadAttempt {
        data class Success(val imageData: ByteArray) : DownloadAttempt
        data object NotFound : DownloadAttempt
        data object Failed : DownloadAttempt
    }

    private companion object {
        const val COVER_BASE_URL =
            "https://raw.githubusercontent.com/jiangxincode/DingooEmu/master/docs/images/"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 20_000
        const val MAX_IMAGE_BYTES = 8L * 1024L * 1024L
        const val COPY_BUFFER_SIZE = 16 * 1024
        const val USER_AGENT = "DingooBoxAndroid/0.2"
        const val LOG_TAG = "DingooCover"
    }
}

private data class DingooCoverCatalogEntry(
    val imageName: String,
    val aliases: Set<String>
)

private object DingooCoverCatalog {
    private val entries = listOf(
        entry("7day-20081217192316.png", "Seven Nights 20081217192316", "七夜 20081217192316"),
        entry("7day-20090715110443.png", "Seven Nights 20090715110443", "七夜 20090715110443"),
        entry(
            "7day-20090715111247.png",
            "Seven Nights 20090715111247",
            "七夜 20090715111247",
            "7day",
            "7 days",
            "seven nights",
            "七夜"
        ),
        entry("Hell_Striker_II-20081229173817.png", "Hell Striker II 20081229173817"),
        entry(
            "Hell_Striker_II-20090122224048.png",
            "Hell Striker II 20090122224048",
            "Hell Striker II",
            "Hell Striker 2",
            "天地道"
        ),
        entry("Rubido-20090512001427.png", "Rubido 20090512001427", "鲁比多 20090512001427"),
        entry(
            "Rubido-20090516230856.png",
            "Rubido 20090516230856",
            "Rubido",
            "鲁比多"
        ),
        entry("Ultimate_Drift-20080716163042.png", "Ultimate Drift 20080716163042"),
        entry(
            "Ultimate_Drift-20081117180631.png",
            "Ultimate Drift 20081117180631",
            "Ultimate Drift",
            "极限漂移"
        ),
        entry("Overlord-Fighter-Stub.png", "Overlord Fighter Stub", "霸王战纪 桩版本"),
        entry("AliBaba.png", "Ali Baba", "阿里巴巴"),
        entry("Astro-Lander__Astro-Lander.png", "Astro Lander", "星际着陆"),
        entry("Block_Breaker.png", "Block Breaker", "打砖块"),
        entry("Candy.png", "Candy", "Candy's House", "Candys House", "糖果屋"),
        entry(
            "Decollation-Warrior.png",
            "Decollation Warrior",
            "God of War Criminal Day",
            "战神刑天"
        ),
        entry("Fomula-One.png", "Fomula One", "Formula One", "F1赛车"),
        entry("GooPlayer__GooPlayer.png", "GooPlayer", "Goo Player", "Goo播放器"),
        entry("Hexa-Virus.png", "Hexa Virus", "六角病毒", "病毒感染"),
        entry("Landlord.png", "Landlord", "斗地主"),
        entry("Link'em_Up.png", "Link'em Up", "Link Em Up", "Mahjong", "连连看"),
        entry("Manic-Miner.png", "Manic Miner", "疯狂矿工"),
        entry("Millipede.png", "Millipede", "千足虫"),
        entry("Mine_Sweeper.png", "Mine Sweeper", "Minesweeper", "扫雷"),
        entry("Mushroom_Roulette.png", "Mushroom Roulette", "蘑菇轮盘"),
        entry("Nose_Breaker.png", "Nose Breaker", "卢比卢比"),
        entry(
            "Overlord-Fighter.png",
            "Overlord Fighter",
            "Yi-Chi King Fighter",
            "Yi Chi King Fighter",
            "霸王战纪"
        ),
        entry("Platinum_Sudoku.png", "Platinum Sudoku", "白金数独"),
        entry("PoPo_Bash.png", "PoPo Bash", "Popo Bash", "Puzzle Bobble", "泡泡"),
        entry("Rick-Dangerous.png", "Rick Dangerous", "里克危险"),
        entry("SameGoo__samegoo.png", "SameGoo", "Same Goo", "消消乐"),
        entry("Snake.png", "Snake", "迪克蛇"),
        entry("Sokuban__Sokuban.png", "Sokuban", "Sokoban", "推箱子"),
        entry("Spoout.png", "Spoout"),
        entry("StopWatch.png", "StopWatch", "Stop Watch", "秒表"),
        entry("Tetris.png", "Tetris", "俄罗斯方块"),
        entry("Zero-Gravity.png", "Zero Gravity", "零重力"),
        entry(
            "Zhao-Chuan_RPG.png",
            "Zhao-Chuan RPG",
            "Zhao Chuan RPG",
            "Zhao Yun Chuan",
            "赵云传"
        ),
        entry("仙剑奇侠传.png", "仙剑奇侠传", "Sword and Fairy")
    )

    fun find(game: GameEntry): DingooCoverCatalogEntry? {
        val requestedNames = listOf(
            game.fileName.substringBeforeLast('.', game.fileName),
            game.title
        )
            .map(::normalize)
            .filter(String::isNotBlank)

        requestedNames.forEach { requestedName ->
            entries.firstOrNull { catalogEntry ->
                catalogEntry.aliases.any { alias -> normalize(alias) == requestedName }
            }?.let { return it }
        }

        return null
    }

    private fun entry(
        imageName: String,
        vararg aliases: String
    ): DingooCoverCatalogEntry = DingooCoverCatalogEntry(
        imageName = imageName,
        aliases = buildSet {
            add(imageName.substringBeforeLast('.'))
            addAll(aliases)
        }
    )

    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(
            value.removeSuffix(".app").removeSuffix(".APP"),
            Normalizer.Form.NFD
        )
        return decomposed
            .replace(COMBINING_MARKS_REGEX, "")
            .lowercase(Locale.ROOT)
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private val COMBINING_MARKS_REGEX = Regex("\\p{M}+")
    private val NON_ALPHANUMERIC_REGEX = Regex("[^\\p{L}\\p{N}]+")
    private val WHITESPACE_REGEX = Regex("\\s+")
}
