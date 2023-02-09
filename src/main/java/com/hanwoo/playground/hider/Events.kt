package com.hanwoo.playground.hider

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import com.hanwoo.playground.*
import com.hanwoo.playground.misc.GlobalLogger
import com.hanwoo.playground.misc.TeamManager.team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.world.InventoryUtils
import org.bukkit.*
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.floor

class Events : Listener {

    private val enteredSpawn = mutableMapOf<UUID, Boolean>()

    @EventHandler
    fun onServerListPing(e: PaperServerListPingEvent) {
        e.setHidePlayers(true)
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        e.joinMessage()?.let {
            Bukkit.getConsoleSender().sendMessage(it)
            GlobalLogger.log(it.text)
            e.player.team?.log(it.text)
            e.player.sendMessage(it)
        }
        e.joinMessage(null)
        val player = e.player
        player.compassTarget = getSpawnLocation(player.uniqueId)
        player.isGlowing = true
        enteredSpawn[player.uniqueId] = false
        Bukkit.getScoreboardManager().mainScoreboard.getTeam("Player")?.addEntry(fakeName)
        playerSession[player.uniqueId] = generateSessionString()

        PacketManager.sendJoinPackets(player)
    }

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        if (e.player.atSpawn) {
            if (enteredSpawn[e.player.uniqueId] == true) return
            enteredSpawn[e.player.uniqueId] = true
            e.player.sendActionBar(
                comps(
                    "Entered Spawn".comp(0xffd84a),
                    " | ".comp(ChatColor.DARK_GRAY),
                    playerSession[e.player.uniqueId]?.comp(0x9eff7a) ?: "NULL".comp()
                )
            )
            e.player.playSound(e.player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        } else {
            if (enteredSpawn[e.player.uniqueId] == false) return
            enteredSpawn[e.player.uniqueId] = false
            e.player.sendActionBar(
                "Left Spawn".comp(0xff9b4a)
            )
        }
    }

    @EventHandler
    fun onSpawn(e: PlayerSpawnLocationEvent) {
        if (e.player.hasPlayedBefore()) return
        e.spawnLocation = getSpawnLocation(e.player.uniqueId)
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        e.quitMessage()?.let {
            Bukkit.getConsoleSender().sendMessage(it)
            GlobalLogger.log(it.text)
            e.player.team?.log(it.text)
            e.player.sendMessage(it)
        }
        e.quitMessage(null)
    }

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        e.deathMessage()?.let {
            Bukkit.getConsoleSender().sendMessage(it)
            GlobalLogger.log(it.text)
            e.player.team?.log(it.text)
        }

        val drops = mutableListOf<ItemStack>()
        e.player.inventory.contents?.filterNotNull()?.forEach { item ->
            val clonedItem = item.clone()
            val amount = item.amount
            repeat(amount) {
                if (Math.random() < 0.5) item.amount--
            }
            clonedItem.amount = amount - item.amount
            drops += clonedItem
        }
        val loc = e.player.location
        drops.filter { it.getEnchantmentLevel(Enchantment.VANISHING_CURSE) == 0 }.forEach {
            InventoryUtils.a((e.player.world as CraftWorld).handle, loc.x, loc.y, loc.z, CraftItemStack.asNMSCopy(it))
        }

        val killer = e.entity.killer
        val flag = (killer is Player) && (killer.uniqueId != e.player.uniqueId)
        val message = "A player died".comp(if (flag) ChatColor.DARK_RED else ChatColor.RED)
            .hoverEvent(
                HoverEvent.showText(
                    LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss")
                    ).comp(ChatColor.GRAY)
                )
            )
        Bukkit.getOnlinePlayers().forEach { it.sendMessage(message) }

        if (flag) e.deathMessage()?.text?.let { killer?.team?.log(it) }
        e.deathMessage(null)
    }

    @EventHandler
    fun onCommandSend(e: PlayerCommandSendEvent) {
        if (e.player.isOp) return
        e.commands.clear()
        plugin.getCommand("e")?.aliases?.let { e.commands.addAll(it) }
        e.commands.add("e")
    }

    @EventHandler
    fun onCommand(e: PlayerCommandPreprocessEvent) {
        if (e.player.isOp) return
        val msg = e.message.removePrefix("/").split(" ")[0]
        if (msg == "e" || plugin.getCommand("e")?.aliases?.contains(msg) == true) return
        e.isCancelled = true
    }

    @EventHandler
    fun onDamageByBlock(e: EntityDamageByBlockEvent) {
        if (e.cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return
        e.damage = e.damage / 2
    }

    @EventHandler
    fun onDamageByEntity(e: EntityDamageByEntityEvent) {
        if (!listOf(
                EntityType.ENDER_CRYSTAL,
                EntityType.PRIMED_TNT,
                EntityType.MINECART_TNT
            ).contains(e.damager.type)
        ) return
        e.damage = e.damage / 2
    }

    @EventHandler
    fun onPlayerSign(e: SignChangeEvent) {
        val block = e.block
        val x = block.x
        val z = block.z

        if (!(x in -15..15 && z in -15..15)) {
            e.lines().forEachIndexed { index, s ->
                e.lines()[index] = s.removeLang().comp()
            }
        }
    }

    @EventHandler
    fun onAdvancement(e: PlayerAdvancementDoneEvent) {
        e.message()?.let {
            Bukkit.getConsoleSender().sendMessage(it)
            e.player.sendMessage(it)
            GlobalLogger.log(it.text)
            e.player.team?.log(it.text)
        }
        e.message(null)
    }

    @EventHandler
    fun onBookEdit(e: PlayerEditBookEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun onPlayerRespawn(e: PlayerRespawnEvent) {
        if (e.isBedSpawn || e.isAnchorSpawn) return
        e.respawnLocation = getSpawnLocation(e.player.uniqueId)
    }

    @EventHandler
    fun onBed(e: PlayerBedEnterEvent) {
        e.setUseBed(Event.Result.DENY)
    }

    @EventHandler
    fun onAnvil(e: PrepareAnvilEvent) {
        e.result?.apply {
            itemMeta = itemMeta.apply {
                this.displayName(this.displayName()?.removeLang()?.comp())
            }
        }
    }

    private fun getSpawnLocation(uuid: UUID): Location {
        val random = Random(uuid.hashCode().toLong() xor spawnSeed.toLong())
        val world = Bukkit.getWorlds().first()
        val size = world.worldBorder.size / 2.0
        val x = random.nextDouble() * size - size / 2.0
        val z = random.nextDouble() * size - size / 2.0
        val block = world.getHighestBlockAt(floor(x).toInt(), floor(z).toInt())
        return block.location.add(0.5, 1.0, 0.5)
    }
}

fun Component.removeLang(): String {
    return this.text.replace("[^ 1-9!-@\\[-`{-~]".toRegex(), "?")
}