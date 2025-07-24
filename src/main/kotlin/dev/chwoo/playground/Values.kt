package dev.chwoo.playground

import dev.chwoo.playground.misc.CooldownManager
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.minecraft.world.item.TooltipFlag
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.Repairable
import org.bukkit.inventory.meta.components.UseCooldownComponent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.io.File
import java.util.UUID
import kotlin.properties.Delegates

var spawnSeed by Delegates.notNull<Int>()
const val fakeName = "???"
val playerSession = mutableMapOf<UUID, String>()
val logsFolder = File(Bukkit.getPluginsFolder().parentFile, "PlaygroundLogs")
val pvpCooldown = mutableMapOf<UUID, Long>()
val emoteEntities = mutableListOf<Entity>()

const val restartInterval: Long = 1000 * 60 * 60 * 12
val restartTime: Long = System.currentTimeMillis() + restartInterval

val playerStealthUntil = mutableMapOf<UUID, Int>()

@Suppress("UnstableApiUsage")
val stealthPotion = ItemStack(Material.POTION).apply {
    itemMeta = (itemMeta as PotionMeta).apply {
        customName("은신의 물약".comp().removeItalic())
        itemName("은신의 물약".comp())
        setRarity(ItemRarity.RARE)
        setEnchantmentGlintOverride(true)
        color = Color.fromRGB(0xbbbbbb)
        addCustomEffect(PotionEffect(PotionEffectType.INVISIBILITY, 20 * 60 * 5, 0, true, true), true)
        setUseCooldown(useCooldown.apply {
            cooldownGroup = CooldownManager.potionCooldownKey
        })
    }
    addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
    lore(listOf(
        "투명 (05:00)".comp(0x84ff6b).removeItalic()
    ))
}