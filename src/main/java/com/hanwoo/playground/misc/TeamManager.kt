package com.hanwoo.playground.misc

import com.hanwoo.playground.logsFolder
import com.hanwoo.playground.misc.GlobalLogger.logFormatter
import com.hanwoo.playground.misc.GlobalLogger.logNameFormatter
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
            val teamName = "Team-$k"

            var num = 1
            var save = "${logNameFormatter.format(Calendar.getInstance().time)}_$num.log"
            var file = Paths.get(logsFolder.toString(), teamName, save)
            while (file.exists()) {
                save = "${logNameFormatter.format(Calendar.getInstance().time)}_${++num}.log"
                file = Paths.get(logsFolder.toString(), teamName, save)
            }

            file.parent.createDirectories()
            val writer = PrintWriter(BufferedWriter(FileWriter(file.toFile(), Charsets.UTF_8)))

            val list = section.getStringList(k)
            teams += GameTeam(k, list.mapNotNull {
                try {
                    UUID.fromString(it)
                } catch (e: Exception) {
                    null
                }
            }, writer)
        }
    }

    fun closeWriters() {
        teams.forEach { it.writer?.close() }
    }

    val OfflinePlayer.team: GameTeam?
        get() = teams.find { it.players.contains(this.uniqueId) }
}

data class GameTeam(val name: String, val players: List<UUID>, val writer: PrintWriter?) {
    fun log(text: String) {
        writer ?: return
        val msg = "${logFormatter.format(Calendar.getInstance().time)} $text"
        writer.println(msg)
        writer.flush()
    }
}