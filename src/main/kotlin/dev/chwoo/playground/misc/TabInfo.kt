package dev.chwoo.playground.misc

import dev.chwoo.playground.comp
import dev.chwoo.playground.comps
import dev.chwoo.playground.getTime
import dev.chwoo.playground.players
import dev.chwoo.playground.restartTime
import dev.chwoo.playground.serverDate
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class TabInfo {
    private val header = comps("PLAYGROUND".comp(0xED4245), Component.newline())
    private val colGood = TextColor.color(99, 242, 109)
    private val colOk = TextColor.color(255, 203, 59)
    private val colBad = TextColor.color(255, 84, 69)
    private val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

    init {
        dateFormatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
    }

    fun run() {
        val dateComp = comps(
            dateFormatter.format(getTime()).comp(0xC9C7C9),
            Component.newline(),
            serverDate()
        )
        val tps = (Bukkit.getTPS()[0] * 10).roundToInt() / 10.0
        val mspt = (Bukkit.getAverageTickTime() * 10).roundToInt() / 10.0
        val timeLeft = (restartTime - System.currentTimeMillis()) / 1000
        val resH = timeLeft / (60 * 60)
        val resM = (timeLeft / 60) % 60
        val resS = timeLeft % 60
        val restartTimeComp = comps(
            Component.newline(),
            "Restart".comp(0xC9C7C9),
            Component.newline(),
            "${resH}:${resM.toString().padStart(2, '0')}:${resS.toString().padStart(2, '0')}".comp(
                restartColor(
                    timeLeft
                )
            )
        )
        players.forEach {
            val serverInfoComp = comps(
                Component.newline(),
                "TPS ".comp(9211020),
                tps.comp(tpsColor(tps)),
                "   MSPT ".comp(9211020),
                mspt.comp(msptColor(mspt)),
                "   PING ".comp(9211020),
                it.ping.comp(pingColor(it.ping)),
            )
            it.sendPlayerListHeaderAndFooter(
                header,
                Component.join(JoinConfiguration.newlines(), dateComp, restartTimeComp, serverInfoComp)
            )
        }
    }

    private fun tpsColor(tps: Double): TextColor {
        return if (tps >= 18.0) colGood
        else if (tps >= 15.0) colOk
        else colBad
    }

    private fun msptColor(mspt: Double): TextColor {
        return if (mspt <= 25.0) colGood
        else if (mspt <= 40.0) colOk
        else colBad
    }

    private fun pingColor(ping: Int): TextColor {
        return if (ping <= 100) colGood
        else if (ping <= 250) colOk
        else colBad
    }

    private fun restartColor(time: Long): TextColor {
        return if (time >= 10 * 60) colGood
        else if (time >= 3 * 60) colOk
        else colBad
    }

}