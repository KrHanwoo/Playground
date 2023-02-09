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
import com.hanwoo.playground.misc.GameTeam
import com.hanwoo.playground.misc.TeamManager.team
import org.bukkit.Bukkit
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
                e.packet.integers.write(1, 0)
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
                    .map { it.profile.fakeProfile(e.player).playerInfoData() })
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
                    val selfChat = comps("[${session}]".comp(0x9eff7a), " $msg".comp())
                    player.sendMessage(selfChat)
                    val publicChat = comps("[${session}]".comp(0xff564a), " $msg".comp())
                    Bukkit.getOnlinePlayers().filter { it.atSpawn && !it.isOp && it.uniqueId != player.uniqueId }
                        .forEach { it.sendMessage(publicChat) }
                    val logChat = comps("[${session}]".comp(0xff564a), " <${player.name}> ".comp(), msg.comp())
                    Bukkit.getOnlinePlayers().filter { it.isOp }.forEach { it.sendMessage(logChat) }
                    Bukkit.getConsoleSender().sendMessage(logChat)
                    return
                }
                player.sendMessage(chat)
                Bukkit.getOnlinePlayers().filter { it.isOp }.forEach { it.sendMessage(chat) }
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

        val team = player.team ?: GameTeam("", listOf(player.uniqueId))
        team.players.forEach { uuid ->
            val plr = Bukkit.getPlayer(uuid) ?: return@forEach
            PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM).apply {
                strings.write(0, "Team")
                integers.write(0, 3)
                getSpecificModifier(Collection::class.java).write(
                    0,
                    team.players.map { Bukkit.getOfflinePlayer(it) }.map { it.name })
                ProtocolLibrary.getProtocolManager().sendServerPacket(plr, this)
            }
        }

        if (!player.isOp) {
            val packet = PacketContainer(PacketType.Play.Server.PLAYER_INFO)
            val playerDataList = mutableListOf<PlayerInfoData>()
            for (offlinePlayer in Bukkit.getOfflinePlayers().asSequence()) {
                val profile = if (offlinePlayer is Player) WrappedGameProfile.fromPlayer(offlinePlayer)
                else WrappedGameProfile.fromOfflinePlayer(offlinePlayer)
                playerDataList += profile.fakeProfile(player).playerInfoData()
            }

            packet.playerInfoActions.write(0, mutableSetOf(EnumWrappers.PlayerInfoAction.ADD_PLAYER))
            packet.playerInfoDataLists.write(1, playerDataList)
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet)
        }
    }
}