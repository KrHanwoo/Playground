package com.hanwoo.playground.misc

import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.FileConfiguration
import java.lang.Exception
import java.util.*

object TeamManager {
    private val teams = mutableListOf<GameTeam>()

    fun loadTeams(config: FileConfiguration) {
        val section = config.getConfigurationSection("teams") ?: return
        section.getKeys(false).forEach { k ->
            val list = section.getStringList(k)
            teams += GameTeam(k, list.mapNotNull {
                try {
                    UUID.fromString(it)
                } catch (e: Exception) {
                    null
                }
            })
        }
    }

    val OfflinePlayer.team: GameTeam?
        get() = teams.find { it.players.contains(this.uniqueId) }
}

data class GameTeam(val name: String, val players: List<UUID>)