package dev.chwoo.playground.misc

import dev.chwoo.playground.comp
import dev.chwoo.playground.loop
import dev.chwoo.playground.players
import dev.chwoo.playground.plugin
import dev.chwoo.playground.restartTime
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration.*
import java.time.Duration
import kotlin.ranges.contains

class RestartTask {
    val titleTime = Title.Times.times(
        Duration.ZERO,
        Duration.ofSeconds(5),
        Duration.ZERO
    )

    fun run() {
        plugin.loop(20) { task() }
    }

    fun task() {
        val timeLeft = (restartTime - System.currentTimeMillis()) / 1000
        if (timeLeft in 60 * 30 - 1..60 * 30) players.forEach {
            it.showTitle(
                Title.title(
                    "".comp(),
                    "서버가 30분 후 재시작됩니다".comp(YELLOW),
                    titleTime
                )
            )
        }
        if (timeLeft in 60 * 15 - 1..60 * 15) players.forEach {
            it.showTitle(
                Title.title(
                    "".comp(),
                    "서버가 15분 후 재시작됩니다".comp(GOLD),
                    titleTime
                )
            )
        }
        if (timeLeft in 5..30)
            players.forEach {
                it.showTitle(
                    Title.title(
                        "".comp(),
                        "서버가 곧 재시작됩니다".comp(RED),
                        titleTime
                    )
                )
            }
        if (timeLeft in 0..5)
            Bukkit.getOnlinePlayers().forEach {
                it.showTitle(
                    Title.title(
                        "서버 재시작".comp(DARK_RED),
                        "".comp(),
                        titleTime
                    )
                )
            }
        if (timeLeft <= 0) Bukkit.restart()
    }
}