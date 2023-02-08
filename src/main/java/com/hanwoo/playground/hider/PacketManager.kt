package com.hanwoo.playground.hider

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.WrappedSignedProperty
import com.hanwoo.playground.comp
import com.hanwoo.playground.fakeProfile
import com.hanwoo.playground.playerInfoData
import com.hanwoo.playground.plugin
import org.bukkit.Bukkit


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
                val chat = "<${e.player.name}> $msg"
                e.player.sendMessage(chat.comp())
                Bukkit.getLogger().info(chat)
            }
        })
    }
}