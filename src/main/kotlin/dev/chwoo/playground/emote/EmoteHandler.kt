package dev.chwoo.playground.emote

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class EmoteHandler: CommandExecutor, TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) return true
        if (args.isEmpty()) return true
        val name = args[0]
        Emote.getEmote(name)?.invoke(sender.location)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String> {
        if (args.size == 1)
            return Emote.emotes.keys.toTypedArray().flatten()
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
                .toMutableList()
        return mutableListOf()
    }
}