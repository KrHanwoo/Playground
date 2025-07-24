package dev.chwoo.playground.misc

import dev.chwoo.playground.comp
import dev.chwoo.playground.loop
import dev.chwoo.playground.players
import dev.chwoo.playground.plugin
import dev.chwoo.playground.restartInterval
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
        if (timeLeft in 5..30)
            players.forEach {
                it.showTitle(
                    Title.title(
                        "".comp(),
                        "Server will restart soon".comp(RED),
                        titleTime
                    )
                )
            }
        if (timeLeft in 0..5)
            Bukkit.getOnlinePlayers().forEach {
                it.showTitle(
                    Title.title(
                        "Server Restart".comp(DARK_RED),
                        "".comp(),
                        titleTime
                    )
                )
            }
        if (timeLeft <= 0) Bukkit.restart()
    }
}