package dev.chwoo.playground

import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.PlayerInfoData
import com.comphenix.protocol.wrappers.WrappedGameProfile
import com.comphenix.protocol.wrappers.WrappedRemoteChatSessionData
import dev.chwoo.playground.hider.skin
import dev.chwoo.playground.misc.TeamManager.team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

val sessionChars = ('A'..'Z') + ('0'..'9')

fun Any.comp(): Component {
    return Component.text(this.toString())
}

fun Any.comp(color: TextColor): Component {
    return this.comp().color(color)
}

fun Any.comp(color: Int): Component {
    return this.comp().color(TextColor.color(color))
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

fun WrappedGameProfile.playerInfoData(gameMode: EnumWrappers.NativeGameMode = EnumWrappers.NativeGameMode.SURVIVAL): PlayerInfoData {
    this.properties.removeAll("textures")
    this.properties.put("textures", skin)
    return PlayerInfoData(
        this.uuid,
        0,
        false,
        gameMode,
        this,
        null,
        null as WrappedRemoteChatSessionData?
    )
}

fun WrappedGameProfile.fakeProfile(player: Player): WrappedGameProfile {
    val flag = player.isOp || player.team.players.contains(uuid)
    if (flag) return this
    return withName(fakeName)
}

val Player.atSpawn: Boolean
    get() {
        if (!(-14..15).contains(this.location.x.toInt())) return false
        if (!(-14..15).contains(this.location.z.toInt())) return false
        return true
    }

fun generateSessionString(): String {
    var str = ""
    repeat(5) {
        str += sessionChars.random()
    }
    return str
}

fun getTime(): Date {
    return Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).time
}

fun getNextRestartTime(): Long {
    val now = LocalDateTime.now()
    var next5am = now.withHour(5).withMinute(0).withSecond(0).withNano(0)
    if (now.isAfter(next5am)) next5am = next5am.plusDays(1)

    return next5am.toEpochSecond(ZoneOffset.of("+09:00")) * 1000
}

fun JavaPlugin.delay(ticks: Long = 0, function: () -> Unit) {
    Bukkit.getScheduler().runTaskLater(this, Runnable { function() }, ticks)
}

fun JavaPlugin.loop(interval: Long = 0, delay: Long = 0, function: () -> Unit) {
    Bukkit.getScheduler().runTaskTimer(this, Runnable { function() }, delay, interval)
}

val UUID.player: Player?
    get() {
        return Bukkit.getPlayer(this)
    }

val players: Collection<Player>
    get() {
        return Bukkit.getOnlinePlayers()
    }