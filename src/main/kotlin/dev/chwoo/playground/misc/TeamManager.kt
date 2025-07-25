package dev.chwoo.playground.misc

import dev.chwoo.playground.getTime
import dev.chwoo.playground.logsFolder
import dev.chwoo.playground.misc.GlobalLogger.logFormatter
import dev.chwoo.playground.misc.GlobalLogger.logNameFormatter
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.FileConfiguration
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.exists


object TeamManager {
    private val teams = mutableListOf<GameTeam>()

    fun loadTeams(config: FileConfiguration) {
        val section = config.getConfigurationSection("teams") ?: return
        section.getKeys(false).forEach { k ->
            val list = section.getStringList(k)
            teams += GameTeam(k, list.mapNotNull {
                try {
                    UUID.fromString(it)
                } catch (_: Exception) {
                    null
                }
            }, getWriter(k, false))
        }
    }

    private fun getWriter(name: String, guest: Boolean): PrintWriter {
        var num = 1
        val folder = if (guest) Paths.get(logsFolder.toString(), "Guests", name)
        else Paths.get(logsFolder.toString(), "Team-$name")

        var save = "${logNameFormatter.format(getTime())}_$num.log"
        var file = Paths.get(folder.toString(), save)
        while (file.exists()) {
            save = "${logNameFormatter.format(getTime())}_${++num}.log"
            file = Paths.get(folder.toString(), save)
        }

        file.parent.createDirectories()
        return PrintWriter(BufferedWriter(FileWriter(file.toFile(), Charsets.UTF_8)))
    }

    fun closeWriters() {
        teams.forEach { it.writer?.close() }
    }

    val OfflinePlayer.team: GameTeam
        get() = teams.firstOrNull { it.players.contains(uniqueId) } ?: GameTeam(
            uniqueId.toString(),
            listOf(uniqueId),
            getWriter(uniqueId.toString(), true),
            true
        ).also { teams += it }

    val OfflinePlayer.hasTeam: Boolean
        get() = teams.any { it.players.contains(uniqueId) && !it.guest }
}

data class GameTeam(val name: String, val players: List<UUID>, val writer: PrintWriter?, val guest: Boolean = false) {
    var compassCooldownUntil: Int = 0

    fun log(text: String) {
        writer ?: return
        val msg = "${logFormatter.format(getTime())} $text"
        writer.println(msg)
        writer.flush()
    }

    fun broadcast(msg: Component) {
        players.mapNotNull { Bukkit.getPlayer(it) }.forEach { it.sendMessage(msg) }
    }
}