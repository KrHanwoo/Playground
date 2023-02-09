package com.hanwoo.playground

import com.comphenix.protocol.wrappers.WrappedSignedProperty
import com.hanwoo.playground.hider.Events
import com.hanwoo.playground.hider.PacketManager
import com.hanwoo.playground.hider.skin
import com.hanwoo.playground.misc.Emote
import com.hanwoo.playground.misc.TabInfo
import com.hanwoo.playground.misc.TeamManager
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team
import java.util.*
import kotlin.properties.Delegates

lateinit var plugin: JavaPlugin
var spawnSeed by Delegates.notNull<Int>()
const val fakeName = "???"
val playerSession = mutableMapOf<UUID, String>()

class Playground : JavaPlugin() {
    init {
        plugin = this
    }

    override fun onEnable() {
        saveDefaultConfig()
        spawnSeed = config.getInt("spawnSeed")
        skin = WrappedSignedProperty("textures", config.getString("skin.textures"), config.getString("skin.signature"))
        TeamManager.loadTeams(config)

        server.pluginManager.registerEvents(Events(), plugin)
        server.scheduler.scheduleSyncRepeatingTask(plugin, TabInfo(), 0, 1)
        PacketManager.register()

        Bukkit.getWorlds().forEach {
            it.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false)
            it.setGameRule(GameRule.KEEP_INVENTORY, true)
            val border = it.worldBorder
            border.center = Location(it, 0.0, 0.0, 0.0)
            border.size = 16000.0
        }
        Bukkit.getWorlds().first().apply {
            spawnLocation = getHighestBlockAt(0, 0).location
        }

        Bukkit.getOnlinePlayers().forEach { playerSession[it.uniqueId] = generateSessionString() }

        createTeam("Player", null)
        createTeam("Team", NamedTextColor.GREEN)
        createTeam("Enemy", NamedTextColor.RED)

        getCommand("e")?.setExecutor(object : CommandExecutor, TabCompleter {
            override fun onCommand(
                sender: CommandSender,
                command: Command,
                label: String,
                args: Array<out String>
            ): Boolean {
                if (sender !is Player) return true
                if (args.isEmpty()) return true
                val name = args[0]
                Emote.getEmote(name)?.invoke(sender.location)
                return true
            }

            override fun onTabComplete(
                sender: CommandSender,
                command: Command,
                label: String,
                args: Array<out String>
            ): MutableList<String> {
                if (args.size == 1)
                    return Emote.emotes.keys.flatten()
                        .filter { it.lowercase().startsWith(args[0].lowercase()) }
                        .toMutableList()
                return mutableListOf()
            }
        })
    }

    private fun createTeam(name: String, color: NamedTextColor?) {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val team = scoreboard.getTeam(name) ?: scoreboard.registerNewTeam(name)
        team.setCanSeeFriendlyInvisibles(false)
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
        team.color(color)
    }
}