package dev.chwoo.playground.misc

import com.comphenix.protocol.PacketType.Play.Server
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.MinecraftKey
import dev.chwoo.playground.comp
import dev.chwoo.playground.fakeName
import dev.chwoo.playground.misc.TeamManager.team
import dev.chwoo.playground.players
import dev.chwoo.playground.plugin
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.collections.get

object CooldownManager {
    val potionCooldownKey = NamespacedKey(plugin, "potion")
    val compassCooldownKey = NamespacedKey(plugin, "compass")

    enum class CooldownItem {
        TOTEM, POTION, COMPASS
    }

    val totemUsedTime = mutableMapOf<UUID, Int>()
    val potionUsedTime = mutableMapOf<UUID, Int>()

    val totemCooldown: Int = 20 * 60 * 5
    val potionCooldown: Int = 20 * 60 * 15
    val compassCooldown: Int = 20 * 60 * 30


    fun canUse(player: Player, item: CooldownItem): Boolean {
        return when (item) {
            CooldownItem.TOTEM -> checkTicks(totemUsedTime[player.uniqueId], totemCooldown)
            CooldownItem.POTION -> checkTicks(potionUsedTime[player.uniqueId], potionCooldown)
            CooldownItem.COMPASS -> player.team.compassCooldownUntil <= Bukkit.getCurrentTick()
        }
    }

    fun use(player: Player, item: CooldownItem) {
        when (item) {
            CooldownItem.TOTEM -> totemUsedTime[player.uniqueId] = Bukkit.getCurrentTick()
            CooldownItem.POTION -> potionUsedTime[player.uniqueId] = Bukkit.getCurrentTick()
            CooldownItem.COMPASS -> player.team.compassCooldownUntil = Bukkit.getCurrentTick() + compassCooldown
        }
        update(player)
    }

    fun update(player: Player) {
        player.setCooldown(Material.TOTEM_OF_UNDYING, getCooldown(totemUsedTime[player.uniqueId], totemCooldown))
        player.setCooldown(potionCooldownKey, getCooldown(potionUsedTime[player.uniqueId], potionCooldown))
        player.setCooldown(compassCooldownKey, getCooldown(player.team.compassCooldownUntil, compassCooldown))
    }

    fun getCooldown(player: Player, item: CooldownItem): Int {
        return when (item) {
            CooldownItem.TOTEM -> getCooldown(totemUsedTime[player.uniqueId], totemCooldown)
            CooldownItem.POTION -> getCooldown(potionUsedTime[player.uniqueId], potionCooldown)
            CooldownItem.COMPASS -> (player.team.compassCooldownUntil - Bukkit.getCurrentTick()).coerceAtLeast(0)
        }
    }

    private fun checkTicks(value: Int?, cooldown: Int): Boolean {
        return (if (value != null) value + cooldown else 0) <= Bukkit.getCurrentTick()
    }

    private fun getCooldown(value: Int?, cooldown: Int): Int {
        return ((if (value != null) value + cooldown else 0) - Bukkit.getCurrentTick()).coerceAtLeast(0)
    }
}