package dev.chwoo.playground.misc

import dev.chwoo.playground.getTime
import dev.chwoo.playground.logsFolder
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Paths
import java.text.SimpleDateFormat
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object GlobalLogger {
    val logFormatter = SimpleDateFormat("[yyyy/MM/dd] [HH:mm:ss]")
    val logNameFormatter = SimpleDateFormat("yyyy-MM-dd")
    private val writer: PrintWriter

    init {
        var num = 1
        var save = "${logNameFormatter.format(getTime())}_$num.log"
        var file = Paths.get(logsFolder.toString(), "Global", save)
        while (file.exists()) {
            save = "${logNameFormatter.format(getTime())}_${++num}.log"
            file = Paths.get(logsFolder.toString(), "Global", save)
        }

        file.parent.createDirectories()
        writer = PrintWriter(BufferedWriter(FileWriter(file.toFile(), Charsets.UTF_8)))
    }

    fun closeWriter() {
        writer.close()
    }

    fun log(text: String) {
        val msg = "${logFormatter.format(getTime())} $text"
        writer.println(msg)
        writer.flush()
    }
}