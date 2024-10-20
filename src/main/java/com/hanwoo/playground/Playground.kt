package com.hanwoo.playground

import com.comphenix.protocol.wrappers.WrappedSignedProperty
import com.hanwoo.playground.emote.EmoteHandler
import com.hanwoo.playground.hider.Events
import com.hanwoo.playground.hider.PacketManager
import com.hanwoo.playground.hider.skin
import com.hanwoo.playground.misc.GlobalLogger
import com.hanwoo.playground.misc.TabInfo
import com.hanwoo.playground.misc.TeamManager
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team
import java.io.File
import java.time.Duration
import java.util.*
import kotlin.properties.Delegates

lateinit var plugin: JavaPlugin
var spawnSeed by Delegates.notNull<Int>()
const val fakeName = "???"
val playerSession = mutableMapOf<UUID, String>()
val logsFolder = File(Bukkit.getPluginsFolder().parentFile, "PlaygroundLogs")
val pvpCooldown = mutableMapOf<UUID, Long>()
var restartTime: Long = System.currentTimeMillis() + 1000 * 60 * 60 * 12

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
        }
        Bukkit.getWorlds().first().apply {
            spawnLocation = getHighestBlockAt(0, 0).location
            setBorderSize(16000)
        }
        Bukkit.getWorlds().first { it.environment == World.Environment.NETHER }.setBorderSize(16000)

        Bukkit.getOnlinePlayers().forEach { playerSession[it.uniqueId] = generateSessionString() }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            pvpCooldown.filter { it.value > System.currentTimeMillis() }.forEach {
                Bukkit.getPlayer(it.key)
                    ?.sendActionBar("PVP MODE".comp(ChatColor.DARK_RED).decorate(TextDecoration.BOLD))
            }
        }, 0, 1)

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            val timeLeft = (restartTime - System.currentTimeMillis()) / 1000
            if ((5..30).contains(timeLeft))
                Bukkit.getOnlinePlayers().forEach {
                    it.showTitle(
                        Title.title(
                            "".comp(),
                            "Server will restart soon".comp(ChatColor.RED),
                            Title.Times.times(
                                Duration.ZERO,
                                Duration.ofSeconds(5),
                                Duration.ZERO
                            )
                        )
                    )
                }
            if ((0..5).contains(timeLeft))
                Bukkit.getOnlinePlayers().forEach {
                    it.showTitle(
                        Title.title(
                            "Server Restart".comp(ChatColor.DARK_RED),
                            "".comp(),
                            Title.Times.times(
                                Duration.ZERO,
                                Duration.ofSeconds(5),
                                Duration.ZERO
                            )
                        )
                    )
                }
            if (timeLeft <= 0) Bukkit.spigot().restart()
        }, 20, 20)

        createTeam("Player", null)
        createTeam("Team", NamedTextColor.GREEN)
        createTeam("Enemy", NamedTextColor.RED)

        getCommand("e")?.setExecutor(EmoteHandler())
    }

    override fun onDisable() {
        TeamManager.closeWriters()
        GlobalLogger.closeWriter()
    }

    private fun createTeam(name: String, color: NamedTextColor?) {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val team = scoreboard.getTeam(name) ?: scoreboard.registerNewTeam(name)
        team.setCanSeeFriendlyInvisibles(false)
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
        team.color(color)
    }

    private fun World.setBorderSize(size: Int) {
        val border = worldBorder
        border.center = Location(this, 0.5, 0.0, 0.5)
        border.size = size + 1.0
    }
}