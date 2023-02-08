package com.hanwoo.playground.misc

import com.hanwoo.playground.comp
import com.hanwoo.playground.comps
import com.hanwoo.playground.serverDate
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import kotlin.math.roundToInt

class TabInfo : Runnable {
    private val header = "PLAYGROUND".comp(0xED4245)
    private val colGood = TextColor.color(99, 242, 109)
    private val colOk = TextColor.color(255, 203, 59)
    private val colBad = TextColor.color(255, 84, 69)

    override fun run() {
        val date = serverDate()
        val tps = (Bukkit.getTPS()[0] * 10).roundToInt() / 10.0
        val mspt = (Bukkit.getAverageTickTime() * 10).roundToInt() / 10.0
        Bukkit.getOnlinePlayers().forEach {
            val serverInfo = comps(
                "TPS ".comp(9211020),
                tps.comp(tpsColor(tps)),
                "   MSPT ".comp(9211020),
                mspt.comp(msptColor(mspt)),
                "   PING ".comp(9211020),
                it.ping.comp(pingColor(it.ping)),
            )
            it.sendPlayerListHeaderAndFooter(header, Component.join(JoinConfiguration.newlines(), date, serverInfo))
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

}