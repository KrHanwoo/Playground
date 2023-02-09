package com.hanwoo.playground.misc

import com.hanwoo.playground.logsFolder
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object GlobalLogger {
    val logFormatter = SimpleDateFormat("[yyyy/MM/dd] [HH:mm:ss]")
    val logNameFormatter = SimpleDateFormat("yyyy-MM-dd")
    private val writer: PrintWriter

    init {
        var num = 1
        var save = "${logNameFormatter.format(Calendar.getInstance().time)}_$num.log"
        var file = Paths.get(logsFolder.toString(), "Global", save)
        while (file.exists()) {
            save = "${logNameFormatter.format(Calendar.getInstance().time)}_${++num}.log"
            file = Paths.get(logsFolder.toString(), "Global", save)
        }

        file.parent.createDirectories()
        writer = PrintWriter(BufferedWriter(FileWriter(file.toFile())))
    }

    fun closeWriter() {
        writer.close()
    }

    fun log(text: String) {
        val msg = "${logFormatter.format(Calendar.getInstance().time)} $text"
        writer.println(msg)
        writer.flush()
    }
}