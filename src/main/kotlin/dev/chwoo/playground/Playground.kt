package dev.chwoo.playground

import com.comphenix.protocol.wrappers.WrappedSignedProperty
import dev.chwoo.playground.emote.EmoteHandler
import dev.chwoo.playground.hider.Events
import dev.chwoo.playground.hider.PacketManager
import dev.chwoo.playground.hider.skin
import dev.chwoo.playground.misc.GlobalLogger
import dev.chwoo.playground.misc.RestartTask
import dev.chwoo.playground.misc.TabInfo
import dev.chwoo.playground.misc.TeamManager
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration.BOLD
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team
import java.io.File


lateinit var plugin: JavaPlugin

class Playground : JavaPlugin() {
    init {
        plugin = this
    }

    override fun onEnable() {
        if(!File(Bukkit.getPluginsFolder(), "Playground").exists()) initConfig()

        TeamManager.loadTeams(config)
        server.pluginManager.registerEvents(Events(), plugin)
        plugin.loop(1) { TabInfo().run() }
        PacketManager.register()
        RestartTask().run()
        init()
    }

    override fun onDisable() {
        emoteEntities.forEach { it.remove() }
        TeamManager.closeWriters()
        GlobalLogger.closeWriter()
    }

    private fun initConfig() {
        saveDefaultConfig()

        Bukkit.getWorlds().forEach {
            it.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false)
            it.setGameRule(GameRule.LOCATOR_BAR, false)
            it.setGameRule(GameRule.KEEP_INVENTORY, true)
        }

        Bukkit.getWorlds().first().apply {
            spawnLocation = getHighestBlockAt(0, 0).location
        }
    }

    private fun init(){
        spawnSeed = config.getInt("spawnSeed")
        skin = WrappedSignedProperty("textures",
            config.getString("skin.textures"),
            config.getString("skin.signature")
        )

        Bukkit.getOnlinePlayers().forEach { playerSession[it.uniqueId] = generateSessionString() }

        plugin.loop(1) {
            pvpCooldown.filter { it.value > System.currentTimeMillis() }.forEach {
                it.key.player?.sendActionBar("PVP MODE".comp(DARK_RED).decorate(BOLD))
            }
        }

        createTeam("Player", null)
        createTeam("Team", GREEN)
        createTeam("Enemy", RED)

        val team = Bukkit.getScoreboardManager().mainScoreboard.getTeam("Player")
        if (team?.hasEntry(fakeName) != true) team?.addEntry(fakeName)
        getCommand("e")?.setExecutor(EmoteHandler())

        registerRecipe()
    }

    private fun registerRecipe() {
        val key = NamespacedKey(this, "stealth_potion")
        val recipe = ShapedRecipe(key, stealthPotion)
        recipe.shape(" P ", "TDW", " S ")
        recipe.setIngredient('P', Material.PRISMARINE_SHARD)
        recipe.setIngredient('T', Material.TURTLE_SCUTE)
        recipe.setIngredient('D', Material.DRAGON_BREATH)
        recipe.setIngredient('W', Material.WITHER_SKELETON_SKULL)
        recipe.setIngredient('S', Material.SCULK_SENSOR)

        server.addRecipe(recipe)
    }

    private fun createTeam(name: String, color: NamedTextColor?) {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val team = scoreboard.getTeam(name) ?: scoreboard.registerNewTeam(name)
        team.setCanSeeFriendlyInvisibles(false)
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
        team.color(color)
    }
}