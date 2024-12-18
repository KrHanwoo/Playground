package com.hanwoo.playground.hider

import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent
import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import com.hanwoo.playground.*
import com.hanwoo.playground.misc.GlobalLogger
import com.hanwoo.playground.misc.TeamManager.team
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.title.Title
import net.minecraft.world.InventoryUtils
import org.bukkit.*
import org.bukkit.craftbukkit.v1_21_R1.CraftWorld
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.Phantom
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.time.Duration
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
            broadcastOP(it)
            GlobalLogger.log(it.text)
            e.player.team.log(it.text)
            e.player.team.broadcast(it)
        }
        e.joinMessage(null)
        val player = e.player
        player.compassTarget = getSpawnLocation(player.uniqueId)
        player.isGlowing = true
        enteredSpawn[player.uniqueId] = false
        playerSession[player.uniqueId] = generateSessionString()

        PacketManager.sendJoinPackets(player)

        spawnNotify(e.player)
    }

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        spawnNotify(e.player)
    }

    @EventHandler
    fun onSpawn(e: PlayerSpawnLocationEvent) {
        if (e.player.hasPlayedBefore()) return
        e.spawnLocation = getSpawnLocation(e.player.uniqueId)
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin) {
            spawnNotify(e.player)
        }
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        e.quitMessage()?.let {
            broadcastOP(it)
            GlobalLogger.log(it.text)
            e.player.team.log(it.text)
            e.player.team.broadcast(it)
        }
        e.quitMessage(null)

        if ((pvpCooldown[e.player.uniqueId] ?: 0) > System.currentTimeMillis()) {
            dropItems(e.player, true)
            pvpCooldown[e.player.uniqueId] = 0
        }
    }

    @EventHandler
    fun onRestart(e: PlayerKickEvent) {
        if (e.cause != PlayerKickEvent.Cause.RESTART_COMMAND) return
        e.reason("Server Restart".comp(0xffed7a))
    }

    @Suppress("DEPRECATION")
    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        e.deathMessage()?.let {
            broadcastOP(it)
            GlobalLogger.log(it.text)
            e.player.team.log(it.text)
        }

        dropItems(e.player)
        pvpCooldown[e.player.uniqueId] = 0

        val hover = HoverEvent.showText(
            LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss")
            ).comp(ChatColor.GRAY)
        )
        val killer = e.entity.killer
        val flag = (killer is Player) && (killer.uniqueId != e.player.uniqueId)
        val message = "A player died".comp(if (flag) ChatColor.DARK_RED else ChatColor.RED).hoverEvent(hover)
        Bukkit.getOnlinePlayers().filter { it.team != e.player.team }.forEach { it.sendMessage(message) }
        val teamMsg = "${e.player.name} died".comp(if (flag) ChatColor.DARK_RED else ChatColor.RED).hoverEvent(hover)
        e.player.team.broadcast(teamMsg)

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
        e.damage /= 2
    }

    @EventHandler
    fun onDamageByEntity(e: EntityDamageByEntityEvent) {
        if (!listOf(
                EntityType.END_CRYSTAL,
                EntityType.TNT,
                EntityType.TNT_MINECART
            ).contains(e.damager.type)
        ) return
        e.damage /= 2
    }

    @EventHandler
    fun onDamageByPlayer(e: EntityDamageByEntityEvent) {
        val player = e.entity
        if (player !is Player) return
        if (e.damager is Projectile) {
            val shooter = (e.damager as Projectile).shooter
            if (shooter !is Player) return
            if (shooter.uniqueId == player.uniqueId) return
            if (shooter.team == player.team) return
            setPvpCooldown(player)
        } else if (e.damager is Player) {
            if ((e.damager as Player).team == player.team) return
            setPvpCooldown(player)
        }
    }

    @Suppress("DEPRECATION")
    private fun setPvpCooldown(player: Player) {
        if ((pvpCooldown[player.uniqueId] ?: 0) < System.currentTimeMillis()) {
            player.showTitle(
                Title.title(
                    "".comp(),
                    "PVP MODE".comp(ChatColor.RED),
                    Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(2), Duration.ofSeconds(1))
                )
            )
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        }
        pvpCooldown[player.uniqueId] = System.currentTimeMillis() + 30 * 1000
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
            broadcastOP(it)
            GlobalLogger.log(it.text)
            e.player.team.log(it.text)
            e.player.team.broadcast(it)
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
        e.player.setStatistic(Statistic.TIME_SINCE_REST, 0)
    }

    @EventHandler
    fun onAnvil(e: PrepareAnvilEvent) {
        e.result?.apply {
            itemMeta = itemMeta.apply {
                this.displayName(this.displayName()?.removeLang()?.comp())
            }
        }
    }

    @EventHandler
    fun onTeleport(e: PlayerTeleportEvent) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin) {
            spawnNotify(e.player)
        }
    }

    @EventHandler
    fun onRespawn(e: PlayerRespawnEvent) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin) {
            spawnNotify(e.player)
        }
    }

    private fun dropItems(player: Player, delay: Boolean = false) {
        val drops = mutableListOf<ItemStack>()
        player.inventory.contents.filterNotNull().forEach { item ->
            val clonedItem = item.clone()
            val amount = item.amount
            repeat(amount) {
                if (Math.random() < 0.5) item.amount--
            }
            clonedItem.amount = amount - item.amount
            drops += clonedItem
        }
        val loc = player.location

        if (delay) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin) {
                itemDrop(drops, loc)
            }
            return
        }
        itemDrop(drops, loc)
    }

    private fun itemDrop(items: List<ItemStack>, loc: Location) {
        items.filter { it.getEnchantmentLevel(Enchantment.VANISHING_CURSE) == 0 }.forEach {
            InventoryUtils.a((loc.world as CraftWorld).handle, loc.x, loc.y, loc.z, CraftItemStack.asNMSCopy(it))
        }
    }

    @Suppress("DEPRECATION")
    private fun spawnNotify(player: Player) {
        if (player.atSpawn) {
            if (enteredSpawn[player.uniqueId] == true) return
            enteredSpawn[player.uniqueId] = true
            player.sendActionBar(
                comps(
                    "Entered Spawn".comp(0xffd84a),
                    " | ".comp(ChatColor.DARK_GRAY),
                    playerSession[player.uniqueId]?.comp(0x9eff7a) ?: "NULL".comp()
                )
            )
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            player.team.broadcast("${player.name} Entered Spawn".comp(0xffd84a))
        } else {
            if (enteredSpawn[player.uniqueId] == false) return
            enteredSpawn[player.uniqueId] = false
            player.sendActionBar(
                "Left Spawn".comp(0xff9b4a)
            )
            player.team.broadcast("${player.name} Left Spawn".comp(0xff9b4a))
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

    private fun broadcastOP(msg: Component) {
        Bukkit.getConsoleSender().sendMessage(msg)
        Bukkit.getOnlinePlayers().filter { it.isOp }.forEach { it.sendMessage(msg) }
    }
}

fun Component.removeLang(): String {
    return this.text.replace("[^ 1-9!-@\\[-`{-~]".toRegex(), "?")
}