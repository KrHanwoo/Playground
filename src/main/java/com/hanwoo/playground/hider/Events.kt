package com.hanwoo.playground.hider

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.*
import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import com.hanwoo.playground.*
import com.hanwoo.playground.misc.Emote
import com.hanwoo.playground.misc.TeamManager.team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.util.NumberConversions
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.floor

class Events : Listener {

    @EventHandler
    fun onServerListPing(e: PaperServerListPingEvent) {
        e.setHidePlayers(true)
    }

    @EventHandler
    fun onPlayerLogin(e: PlayerLoginEvent) {
        if (e.result == PlayerLoginEvent.Result.KICK_FULL) e.allow()
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        e.joinMessage()?.let {
            Bukkit.getLogger().info(it.text)
            e.player.sendMessage(it)
        }
        e.joinMessage(null)
        val player = e.player
        player.compassTarget = Bukkit.getWorlds().first().spawnLocation
        player.isGlowing = true
        Bukkit.getScoreboardManager().mainScoreboard.getTeam("Player")?.addEntry(fakeName)

        val team = player.team
        team?.players?.forEach { uuid ->
            val plr = Bukkit.getPlayer(uuid) ?: return@forEach
            PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM).apply {
                strings.write(0, "Team")
                integers.write(0, 3)
                getSpecificModifier(Collection::class.java).write(0,team.players.map { Bukkit.getOfflinePlayer(it) }.map { it.name })
                ProtocolLibrary.getProtocolManager().sendServerPacket(plr, this)
            }
        }

        if (!player.isOp) {
            val packet = PacketContainer(PacketType.Play.Server.PLAYER_INFO)
            val playerDataList = mutableListOf<PlayerInfoData>()
            for (offlinePlayer in Bukkit.getOfflinePlayers().asSequence()) {
                val profile = if (offlinePlayer is Player) WrappedGameProfile.fromPlayer(offlinePlayer)
                else WrappedGameProfile.fromOfflinePlayer(offlinePlayer)
                playerDataList += profile.fakeProfile(e.player).playerInfoData()
            }

            packet.playerInfoActions.write(0, mutableSetOf(EnumWrappers.PlayerInfoAction.ADD_PLAYER))
            packet.playerInfoDataLists.write(1, playerDataList)
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet)
        }
    }

    @EventHandler
    fun onSpawn(e: PlayerSpawnLocationEvent) {
        if (e.player.hasPlayedBefore()) return
        e.spawnLocation = getSpawnLocation(e.player.uniqueId)
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        e.quitMessage()?.let {
            Bukkit.getLogger().info(it.text)
            e.player.sendMessage(it)
        }
        e.quitMessage(null)
    }

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        e.deathMessage()?.let { Bukkit.getLogger().info(it.text) }
        e.deathMessage(null)

        val message = when (e.entity.killer) {
            is Player -> "A player died".comp(ChatColor.DARK_RED)
            else -> "A player died".comp(ChatColor.RED)
        }.hoverEvent(
            HoverEvent.showText(
                LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss")
                ).comp(ChatColor.GRAY)
            )
        )

        Bukkit.getOnlinePlayers().forEach { it.sendMessage(message) }
    }

    @EventHandler
    fun onCommand(e: PlayerCommandSendEvent) {
        e.commands.clear()
        plugin.getCommand("e")?.aliases?.let { e.commands.addAll(it) }
        e.commands.add("e")
    }

    @EventHandler
    fun onPlayerSign(e: SignChangeEvent) {
        val block = e.block
        val x = block.x
        val z = block.z

        if (!(x in -15..15 && z in -15..15)) {
            e.lines().forEachIndexed { index, s ->
                e.lines()[index] = s.removeLang().comp()
            }
        }
    }

    @EventHandler
    fun onAdvancement(e: PlayerAdvancementDoneEvent) {
        e.message()?.let {
            Bukkit.getLogger().info(it.text)
            e.player.sendMessage(it)
        }
        e.message(null)
    }

    @EventHandler
    fun onBookEdit(e: PlayerEditBookEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun onPlayerRespawn(e: PlayerRespawnEvent) {
        if (e.isBedSpawn || e.isAnchorSpawn) return
        e.respawnLocation = getSpawnLocation(e.player.uniqueId)
    }

    @EventHandler
    fun onBed(e: PlayerBedEnterEvent) {
        e.setUseBed(Event.Result.DENY)
    }

    private fun getSpawnLocation(uuid: UUID): Location {
        val random = Random(uuid.hashCode().toLong() xor spawnSeed.toLong())
        val world = Bukkit.getWorlds().first()
        val size = world.worldBorder.size / 2.0
        val x = random.nextDouble() * size - size / 2.0
        val z = random.nextDouble() * size - size / 2.0
        val block = world.getHighestBlockAt(floor(x).toInt(), floor(z).toInt())
        return block.location.add(0.5, 1.0, 0.5)
    }
}

fun Component.removeLang(): String {
    return this.text.replace("[^ 1-9!-@\\[-`{-~]".toRegex(), "?")
}