package com.hanwoo.playground.hider

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.PlayerInfoData
import com.comphenix.protocol.wrappers.WrappedGameProfile
import com.comphenix.protocol.wrappers.WrappedSignedProperty
import com.hanwoo.playground.*
import com.hanwoo.playground.misc.GlobalLogger
import com.hanwoo.playground.misc.TeamManager.hasTeam
import com.hanwoo.playground.misc.TeamManager.team
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket
import net.minecraft.server.level.ClientInformation
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player


lateinit var skin: WrappedSignedProperty

object PacketManager {
    fun register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(object : PacketAdapter(
            plugin,
            ListenerPriority.NORMAL,
            PacketType.Play.Client.SETTINGS
        ) {
            override fun onPacketReceiving(e: PacketEvent) {
                val x = e.packet.structures.read(0)
                x.integers.write(1, 0)
                e.packet.structures.write(0, x)
            }
        })

        ProtocolLibrary.getProtocolManager().addPacketListener(object : PacketAdapter(
            plugin,
            ListenerPriority.NORMAL,
            PacketType.Play.Server.PLAYER_INFO
        ) {
            override fun onPacketSending(e: PacketEvent) {
                val dataLists = e.packet.playerInfoDataLists
                dataLists.write(1, dataLists.read(1)
                    .map { it.profile.fakeProfile(e.player).playerInfoData(it.gameMode) })
            }
        })

        ProtocolLibrary.getProtocolManager().addPacketListener(object : PacketAdapter(
            plugin,
            ListenerPriority.NORMAL,
            PacketType.Play.Server.PLAYER_INFO_REMOVE
        ) {
            override fun onPacketSending(e: PacketEvent) {
                if (e.player.isOp) return
                e.isCancelled = true
            }
        })

        ProtocolLibrary.getProtocolManager().addPacketListener(object : PacketAdapter(
            plugin,
            ListenerPriority.NORMAL,
            PacketType.Play.Client.CHAT
        ) {
            override fun onPacketReceiving(e: PacketEvent) {
                e.isCancelled = true
                val msg = e.packet.strings.readSafely(0)
                msg ?: return
                val player = e.player
                val chat = "<${player.name}> $msg".comp()
                if (player.isOp) {
                    Bukkit.broadcast(chat)
                    return
                }
                if (player.atSpawn) {
                    val session = playerSession[player.uniqueId] ?: return
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
                    Bukkit.getOnlinePlayers().filter { it.atSpawn && !it.isOp && it.uniqueId != player.uniqueId }
                        .forEach { it.sendMessage(publicChat) }
                    val logChat = comps(
                        "[${dimensionName}] [${session}]".comp(dimensionColor),
                        " <${player.name}> ".comp(),
                        msg.comp()
                    )
                    Bukkit.getOnlinePlayers().filter { it.isOp }.forEach { it.sendMessage(logChat) }
                    Bukkit.getConsoleSender().sendMessage(logChat)
                    GlobalLogger.log(logChat.text)
                    return
                }
                player.team.broadcast(chat)
                player.team.log(chat.text)

                if (player.hasTeam) {
                    val teamChat = "[Team-${player.team.name}] <${player.name}> $msg".comp()
                    Bukkit.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }
                        .filter { it.isOp }.forEach { it.sendMessage(teamChat) }
                    Bukkit.getConsoleSender().sendMessage(teamChat)
                    return
                }
                Bukkit.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }
                    .filter { it.isOp }.forEach { it.sendMessage(chat) }
                Bukkit.getConsoleSender().sendMessage(chat)
            }
        })
    }

    fun sendJoinPackets(player: Player) {
        PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM).apply {
            strings.write(0, "Enemy")
            integers.write(0, 3)
            getSpecificModifier(Collection::class.java).write(
                0,
                listOf(fakeName)
            )
            Bukkit.getOnlinePlayers().forEach {
                ProtocolLibrary.getProtocolManager().sendServerPacket(it, this)
            }
        }

        if (!player.isOp) {
            val packet = PacketContainer(PacketType.Play.Server.PLAYER_INFO)
            val playerDataList = mutableListOf<PlayerInfoData>()
            for (offlinePlayer in Bukkit.getOfflinePlayers().asSequence()) {
                val profile = if (offlinePlayer is Player) WrappedGameProfile.fromPlayer(offlinePlayer)
                else WrappedGameProfile.fromOfflinePlayer(offlinePlayer)
                playerDataList += profile.withName(offlinePlayer.name).fakeProfile(player).playerInfoData()
            }

            packet.playerInfoActions.write(0, mutableSetOf(EnumWrappers.PlayerInfoAction.ADD_PLAYER))
            packet.playerInfoDataLists.write(1, playerDataList)
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet)
        }

        val team = player.team
        team.players.forEach { uuid ->
            val plr = Bukkit.getPlayer(uuid) ?: return@forEach
            PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM).apply {
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