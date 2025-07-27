package dev.chwoo.playground.hider

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.PacketType.Play.*
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.injector.netty.WirePacket
import com.comphenix.protocol.reflect.FuzzyReflection
import com.comphenix.protocol.reflect.accessors.Accessors
import com.comphenix.protocol.reflect.accessors.MethodAccessor
import com.comphenix.protocol.utility.MinecraftReflection
import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.PlayerInfoData
import com.comphenix.protocol.wrappers.WrappedDataValue
import com.comphenix.protocol.wrappers.WrappedGameProfile
import com.comphenix.protocol.wrappers.WrappedSignedProperty
import dev.chwoo.playground.*
import dev.chwoo.playground.misc.CooldownManager
import dev.chwoo.playground.misc.GlobalLogger
import dev.chwoo.playground.misc.TeamManager.hasTeam
import dev.chwoo.playground.misc.TeamManager.team
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import kotlin.experimental.or


lateinit var skin: WrappedSignedProperty

object PacketManager {
    fun register() {
        addReceiver(Client.SETTINGS) { e ->
            val x = e.packet.structures.read(0)
            x.integers.write(1, 0)
            e.packet.structures.write(0, x)
        }

        addSender(Server.PLAYER_INFO) { e ->
            val dataLists = e.packet.playerInfoDataLists
            val newData = dataLists.read(1).map {
                it.profile.fakeProfile(e.player).playerInfoData(it.gameMode, e.player)
            }
            dataLists.write(1, newData)
        }

        addSender(Server.PLAYER_INFO_REMOVE) { e ->
            if (e.player.isOp) return@addSender
            e.isCancelled = true
        }

        addReceiver(Client.CHAT) { e ->
            e.isCancelled = true
            val msg = e.packet.strings.readSafely(0)
            msg ?: return@addReceiver

            val player = e.player
            val chat = "<${player.name}> $msg".comp()
            if (player.isOp) {
                Bukkit.broadcast(chat)
                GlobalLogger.log(chat.text)
                player.team.log(chat.text)
                return@addReceiver
            }
            if (player.atSpawn) {
                val session = playerSession[player.uniqueId] ?: return@addReceiver
                val selfChat = comps("[${session}]".comp(0xfcdd3f), " $msg".comp())
                player.sendMessage(selfChat)
                val dimensionColor = when (player.world.environment) {
                    World.Environment.NORMAL -> 0x65f046
                    World.Environment.NETHER -> 0xff5c40
                    World.Environment.THE_END -> 0xb65cff
                    else -> 0xbfbfbf
                }
                val dimensionName = when (player.world.environment) {
                    World.Environment.NORMAL -> "OVERWORLD"
                    World.Environment.NETHER -> "NETHER"
                    World.Environment.THE_END -> "END"
                    else -> "NULL"
                }
                val publicChat = comps("[${session}]".comp(dimensionColor), " $msg".comp())
                players.filter { !it.isOp && it.uniqueId != player.uniqueId }
                    .forEach { it.sendMessage(publicChat) }
                val logChat = comps(
                    "[${dimensionName}] [${session}]".comp(dimensionColor),
                    " <${player.name}> ".comp(),
                    msg.comp()
                )
                players.filter { it.isOp }.forEach { it.sendMessage(logChat) }
                Bukkit.getConsoleSender().sendMessage(logChat)
                GlobalLogger.log(logChat.text)
                return@addReceiver
            }

            player.team.broadcast(chat)
            player.team.log(chat.text)

            var opChat = chat
            if (player.hasTeam)
                opChat = "[Team-${player.team.name}] <${player.name}> $msg".comp()

            GlobalLogger.log(opChat.text)
            players.filter { it.uniqueId != player.uniqueId }
                .filter { it.isOp }.forEach { it.sendMessage(opChat) }
            Bukkit.getConsoleSender().sendMessage(opChat)
        }
    }

    fun sendJoinPackets(player: Player) {
        PacketContainer(Server.SCOREBOARD_TEAM).apply {
            strings.write(0, "Enemy")
            integers.write(0, 3)
            getSpecificModifier(Collection::class.java).write(
                0,
                listOf(fakeName)
            )
            players.forEach {
                ProtocolLibrary.getProtocolManager().sendServerPacket(it, this)
            }
        }

        if (!player.isOp) {
            val packet = PacketContainer(Server.PLAYER_INFO)
            val playerDataList = mutableListOf<PlayerInfoData>()
            for (offlinePlayer in Bukkit.getOfflinePlayers().asSequence()) {
                val profile = if (offlinePlayer is Player) WrappedGameProfile.fromPlayer(offlinePlayer)
                else WrappedGameProfile.fromOfflinePlayer(offlinePlayer)
                playerDataList += profile.withName(offlinePlayer.name).fakeProfile(player)
                    .playerInfoData()
            }

            packet.playerInfoActions.write(0, mutableSetOf(EnumWrappers.PlayerInfoAction.ADD_PLAYER))
            packet.playerInfoDataLists.write(1, playerDataList)
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet)
        }

        val team = player.team
        team.players.forEach { uuid ->
            val plr = Bukkit.getPlayer(uuid) ?: return@forEach
            PacketContainer(Server.SCOREBOARD_TEAM).apply {
                strings.write(0, "Team")
                integers.write(0, 3)
                getSpecificModifier(Collection::class.java).write(
                    0,
                    team.players.map { Bukkit.getOfflinePlayer(it) }.mapNotNull { it.name })
                ProtocolLibrary.getProtocolManager().sendServerPacket(plr, this)
            }
        }
    }
}

fun addReceiver(type: PacketType, function: (e: PacketEvent) -> Unit) {
    ProtocolLibrary.getProtocolManager().addPacketListener(object : PacketAdapter(
        plugin, ListenerPriority.NORMAL, type
    ) {
        override fun onPacketReceiving(e: PacketEvent) {
            function(e)
        }
    })
}

fun addSender(type: PacketType, function: (e: PacketEvent) -> Unit) {
    ProtocolLibrary.getProtocolManager().addPacketListener(object : PacketAdapter(
        plugin, ListenerPriority.NORMAL, type
    ) {
        override fun onPacketSending(e: PacketEvent) {
            function(e)
        }
    })
}

