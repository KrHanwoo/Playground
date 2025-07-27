package dev.chwoo.playground.hider

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import dev.chwoo.playground.*
import dev.chwoo.playground.misc.CooldownManager
import dev.chwoo.playground.misc.CooldownManager.getCooldown
import dev.chwoo.playground.misc.CooldownManager.potionCooldownKey
import dev.chwoo.playground.misc.GlobalLogger
import dev.chwoo.playground.misc.TeamManager.hasTeam
import dev.chwoo.playground.misc.TeamManager.team
import dev.chwoo.playground.text
import io.papermc.paper.event.entity.TameableDeathMessageEvent
import io.papermc.paper.event.player.PlayerItemGroupCooldownEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.title.Title
import net.minecraft.world.Containers
import org.bukkit.*
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
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
import kotlin.math.round

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
            if (e.player.isOp) return@let
            e.player.team.log(it.text)
            e.player.team.broadcast(it)
        }
        e.joinMessage(null)
        val player = e.player
        player.compassTarget = Bukkit.getWorlds().first().spawnLocation
        player.isGlowing = (playerStealthUntil[player.uniqueId] ?: 0) <= Bukkit.getCurrentTick()
        enteredSpawn[player.uniqueId] = false
        playerSession[player.uniqueId] = generateSessionString()

        PacketManager.sendJoinPackets(player)
        spawnNotify(e.player)
        CooldownManager.update(e.player)

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
            if (e.player.isOp) return@let
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
        e.reason("서버 재시작".comp(0xffed7a))
    }

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
            ).comp(GRAY)
        )
        val killer = e.entity.killer
        val flag = (killer is Player) && (killer.uniqueId != e.player.uniqueId)
        val message = "누군가 죽었다.".comp(if (flag) DARK_RED else RED).hoverEvent(hover)
        Bukkit.getOnlinePlayers().filter { it.team != e.player.team }.forEach { it.sendMessage(message) }
        val teamMsg = "${e.player.name} 사망".comp(if (flag) DARK_RED else RED).hoverEvent(hover)
        e.player.team.broadcast(teamMsg)

        if (flag) e.deathMessage()?.text?.let { killer.team.log(it) }
        e.deathMessage(null)
        playerStealthUntil[e.entity.uniqueId] = 0
        e.player.isGlowing = true
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

    @EventHandler
    fun onResurrect(e: EntityResurrectEvent) {
        if (e.entity !is Player) return
        if (e.isCancelled) return
        val player = e.entity as Player
        if (!CooldownManager.canUse(player, CooldownManager.CooldownItem.TOTEM)) {
            e.isCancelled = true
            return
        }
        CooldownManager.use(player, CooldownManager.CooldownItem.TOTEM)
        playerStealthUntil[e.entity.uniqueId] = 0
        e.entity.isGlowing = true
    }

    private fun setPvpCooldown(player: Player) {
        if ((pvpCooldown[player.uniqueId] ?: 0) < System.currentTimeMillis()) {
            player.showTitle(
                Title.title(
                    "".comp(),
                    "전투 상태".comp(RED),
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
            if (e.player.isOp) return@let
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
        if (e.inventory.contents.first()?.isSimilar(stealthPotion) == true) e.result = null
        if (e.inventory.contents.first()?.isSimilar(tracker) == true) e.result = null

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
        if (e.from.world == e.to.world) return

        plugin.delay(0) {
            CooldownManager.update(e.player)
        }
    }

    @EventHandler
    fun onRespawn(e: PlayerRespawnEvent) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin) {
            spawnNotify(e.player)
        }

        plugin.delay(0) {
            CooldownManager.update(e.player)
        }
    }

    @EventHandler
    fun onOpenInventory(e: InventoryOpenEvent) {
        if (e.inventory.type != InventoryType.ENDER_CHEST) return
        e.isCancelled = true
    }

    @EventHandler
    fun onConsume(e: PlayerItemConsumeEvent) {
        if (!e.item.isSimilar(stealthPotion)) return
        if (CooldownManager.canUse(
                e.player,
                CooldownManager.CooldownItem.POTION
            ) && (restartTime - System.currentTimeMillis()) / 1000 > 60 * 15
        ) {
            CooldownManager.use(e.player, CooldownManager.CooldownItem.POTION)
            e.player.isGlowing = false
            playerStealthUntil[e.player.uniqueId] = Bukkit.getCurrentTick() + 20 * 60 * 5
            plugin.delay(20 * 60 * 5) {
                e.player.isGlowing = (playerStealthUntil[e.player.uniqueId] ?: 0) <= Bukkit.getCurrentTick()
            }
        } else {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onEffectChange(e: EntityPotionEffectEvent) {
        if (e.entity !is Player) return
        if (e.cause != EntityPotionEffectEvent.Cause.MILK) return
        playerStealthUntil[e.entity.uniqueId] = 0
        e.entity.isGlowing = true
    }

    @EventHandler
    fun onCooldown(e: PlayerItemGroupCooldownEvent) {
        if (e.cooldownGroup == potionCooldownKey) {
            e.cooldown = getCooldown(e.player, CooldownManager.CooldownItem.POTION)
        } else if (e.cooldownGroup == CooldownManager.compassCooldownKey) {
            e.cooldown = getCooldown(e.player, CooldownManager.CooldownItem.COMPASS)
        }
    }

    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        val player = e.player
        plugin.delay {
            if (!player.isHandRaised) return@delay
            val hand = e.hand ?: return@delay
            val item = player.inventory.getItem(hand)
            if (!item.isSimilar(tracker)) return@delay
            if (!CooldownManager.canUse(player, CooldownManager.CooldownItem.COMPASS)) return@delay
            val targets = players.filter {
                !it.isOp && it.gameMode == GameMode.SURVIVAL && !player.team.players.contains(it.uniqueId)
            }
            val nearest = targets.filter { player.location.world == it.location.world }
                .minOfOrNull { player.location.distance(it.location) }
            if (nearest == null || (restartTime - System.currentTimeMillis()) / 1000 <= 60 * 30) {
                player.location.world.playSound(player, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
                player.team.compassCooldownUntil = Bukkit.getCurrentTick() + 20
            } else {
                player.location.world.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.5f, .5f)
                player.location.world.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, .9f)
                CooldownManager.use(player, CooldownManager.CooldownItem.COMPASS)
                val chat = comps(
                    "[${player.name}] ".comp(),
                    "가장 가까운 적까지의 거리: ".comp(0xfcffad),
                    "${"%,d".format(round(nearest).toInt())}블록".comp(0x8cddff)
                )
                player.team.broadcast(chat)

                var opChat = chat
                if (player.hasTeam)
                    opChat = comps("[Team-${player.team.name}] ".comp(), chat)

                player.team.log(chat.text)
                GlobalLogger.log(opChat.text)
                players.filter { it.uniqueId != player.uniqueId }
                    .filter { it.isOp }.forEach { it.sendMessage(opChat) }
                Bukkit.getConsoleSender().sendMessage(opChat)
            }
            player.team.players.forEach { it.player?.let { p -> CooldownManager.update(p) } }
        }
    }

    @EventHandler
    fun onTameableDeathMessage(e: TameableDeathMessageEvent){
        e.isCancelled = true
        val killer = e.entity.killer
        val owner = e.entity.owner
        val msg = e.deathMessage()
        broadcastOP(msg)
        GlobalLogger.log(msg.text)
        if (killer is Player) killer.team.log(msg.text)
        if (owner is Player) owner.team.log(msg.text)
    }

    private fun dropItems(player: Player, delay: Boolean = false) {
        val drops = mutableListOf<ItemStack>()
        player.inventory.contents.filterNotNull().forEach { item ->
            if (item.isSimilar(tracker)) {
                item.amount = 0
                return@forEach
            }
            val clonedItem = item.clone()
            val amount = item.amount
            repeat(amount) {
                if (Math.random() < 0.5) item.amount--
            }
            clonedItem.amount = amount - item.amount
            drops += clonedItem
        }
        val loc = player.location

        if (delay) return plugin.delay {
            itemDrop(drops, loc)
        }
        itemDrop(drops, loc)
    }

    private fun itemDrop(items: List<ItemStack>, loc: Location) {
        items.filter { it.getEnchantmentLevel(Enchantment.VANISHING_CURSE) == 0 }.forEach {
            Containers.dropItemStack(
                (loc.world as CraftWorld).handle,
                loc.x,
                loc.y,
                loc.z,
                CraftItemStack.asNMSCopy(it)
            )
        }
    }

    private fun spawnNotify(player: Player) {
        if (player.atSpawn) {
            if (enteredSpawn[player.uniqueId] == true) return
            enteredSpawn[player.uniqueId] = true
            player.sendActionBar(
                comps(
                    "스폰 입장".comp(0xffd84a),
                    " | ".comp(DARK_GRAY),
                    playerSession[player.uniqueId]?.comp(0x9eff7a) ?: "NULL".comp()
                )
            )
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            val msg = "${player.name} 스폰 입장".comp(0xffd84a)
            player.team.broadcast(msg)
            player.team.log(msg.text)
            GlobalLogger.log(msg.text)
        } else {
            if (enteredSpawn[player.uniqueId] == false) return
            enteredSpawn[player.uniqueId] = false
            player.sendActionBar(
                "스폰 퇴장".comp(0xff9b4a)
            )
            val msg = "${player.name} 스폰 퇴장".comp(0xff9b4a)
            player.team.broadcast(msg)
            player.team.log(msg.text)
            GlobalLogger.log(msg.text)
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
    return this.text.replace("[^ ]".toRegex(), "?")
}