package com.hanwoo.playground

import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.PlayerInfoData
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.comphenix.protocol.wrappers.WrappedGameProfile
import com.hanwoo.playground.hider.skin
import com.hanwoo.playground.misc.TeamManager.team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player

fun Any.comp(): Component {
    return Component.text(this.toString())
}

fun Any.comp(color: TextColor): Component {
    return this.comp().color(color)
}

fun Any.comp(color: Int): Component {
    return this.comp().color(TextColor.color(color))
}

fun Any.comp(color: ChatColor): Component {
    return this.comp().color(TextColor.color(color.asBungee().color.rgb))
}

fun comps(vararg component: Component): Component {
    return Component.join(
        JoinConfiguration.noSeparators(),
        *component
    )
}

val Component.len: Int
    get() {
        return this.text.length
    }

val Component.text: String
    get() {
        return PlainTextComponentSerializer.plainText().serialize(this)
    }


fun Component.removeItalic(): Component {
    return this.decoration(TextDecoration.ITALIC, false)
}

fun serverDate(): Component {
    val days = Bukkit.getServer().worlds[0].fullTime / 24000
    val year = days / 365
    val day = days % 365
    return comps(
        "Year ".comp(0xC9C7C9),
        year.comp(0xFFFA75),
        " Day ".comp(0xC9C7C9),
        day.comp(0xFFFA75)
    )
}

fun WrappedGameProfile.playerInfoData(): PlayerInfoData {
    this.properties.removeAll("textures")
    this.properties.put("textures", skin)
    return PlayerInfoData(
        this.uuid,
        0,
        false,
        EnumWrappers.NativeGameMode.SURVIVAL,
        this,
        WrappedChatComponent.fromText(""),
        null
    )
}

fun WrappedGameProfile.fakeProfile(player: Player): WrappedGameProfile {
    val flag = player.isOp || player.team?.players?.contains(this.uuid) == true
    if (flag && this.name != null) return this
    return this.withName(fakeName)
}