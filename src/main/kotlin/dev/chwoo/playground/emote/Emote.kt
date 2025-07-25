package dev.chwoo.playground.emote

import dev.chwoo.playground.comp
import dev.chwoo.playground.emoteEntities
import dev.chwoo.playground.plugin
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.Quaternionf
import org.joml.Vector3f

object Emote {
    val emotes = mutableMapOf<Array<out String>, (Location) -> Unit>()

    private fun register(vararg names: String, emote: (Location) -> Unit) {
        emotes[names] = emote
    }

    fun getEmote(name: String): ((Location) -> Unit)? {
        return emotes.filter { it.key.contains(name.lowercase()) }.map { it.value }.firstOrNull()
    }

    private fun Location.headParticle(particle: Particle) {
        this.add(0.0, 2.0, 0.0).spreadParticle(particle)
    }

    private fun Location.spreadParticle(particle: Particle, count: Int = 1, spread: Double = 0.0, extra: Double = 0.0) {
        world.spawnParticle(
            particle,
            x,
            y,
            z,
            count,
            spread,
            spread,
            spread,
            extra,
            null,
            true
        )
    }

    init {
        register("angry", "grrr") {
            it.headParticle(Particle.ANGRY_VILLAGER)
        }
        register("love", "heart") {
            it.headParticle(Particle.HEART)
        }
        register("damage", "ouch") {
            it.headParticle(Particle.DAMAGE_INDICATOR)
        }
        register("critical", "puk") {
            it.spreadParticle(Particle.DAMAGE_INDICATOR, 16, 0.5)
        }
        register("spit") {
            val v = it.direction
            it.world.spawnParticle(
                Particle.SPIT,
                it.x,
                it.y + 1.62,
                it.z,
                0,
                v.x,
                v.y,
                v.z,
                1.0,
                null,
                true
            )
            it.world.playSound(it, Sound.ENTITY_LLAMA_SPIT, 1.0F, 1.0F)
        }
        register("no", "ban") {
            it.world.spawnParticle(
                Particle.BLOCK_MARKER,
                it.x,
                it.y + 2.5,
                it.z,
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                Material.BARRIER.createBlockData(),
                true
            )
            it.world.playSound(it, Sound.BLOCK_ANVIL_LAND, 0.5F, 0.1F)
        }
        register("note", "music") {
            it.headParticle(Particle.NOTE)
        }
        register("rage", "grrrrr") {
            it.spreadParticle(Particle.LAVA, 50)
            it.world.playSound(it, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F)
        }
        register("tear", "sob", "sad") {
            for (i in 0..1) {
                val v = Vector(-0.1 + i * 0.2, 0.1, 0.4)
                v.rotateAroundX(Math.toRadians(it.pitch.toDouble())).rotateAroundY(Math.toRadians(-it.yaw.toDouble()))
                it.clone().add(v.x, 1.3 + v.y, v.z).spreadParticle(Particle.FALLING_WATER, 1, 0.0, 1.0)
            }
        }
        register("ojjol") {
            var idx = 0
            val txt = listOf(
                "어".comp(0xff0000).decorate(TextDecoration.BOLD),
                "쩔".comp(0x12bf00).decorate(TextDecoration.BOLD),
                "미".comp(0x0000ff).decorate(TextDecoration.BOLD),
                "모".comp(0xff00ff).decorate(TextDecoration.BOLD)
            )
            for (i in intArrayOf(1, -1)) {
                for (j in intArrayOf(-1, 1)) {
                    val front = it.direction.clone().setY(0).normalize().multiply(0.5)
                    val side = it.direction.clone().setY(0).normalize().rotateAroundY(Math.PI / 2).multiply(0.5)
                    val top = Vector(0.0, 0.5, 0.0)
                    val loc = it.clone().add(0.0, 1.0, 0.0).add(front).add(side.clone().multiply(j))
                        .add(top.clone().multiply(i))
                    val d = it.world.createEntity(loc, TextDisplay::class.java)
                    emoteEntities.add(d)
                    d.text(txt[idx++])
                    d.billboard = Display.Billboard.CENTER
                    d.backgroundColor = Color.fromRGB(255, 255, 255)
                    d.teleportDuration = 10
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                        d.spawnAt(loc)
                        d.teleport(loc.add(0.0, 0.5, 0.0))
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                            d.interpolationDelay = 0
                            d.interpolationDuration = 15
                            d.transformation = Transformation(
                                Vector3f(0f,0f,0f),
                                Quaternionf(0f,0f,0f,1f),
                                Vector3f(0f,0f,0f),
                                Quaternionf(0f,0f,0f,1f)
                            )
                            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                                d.remove()
                                emoteEntities.remove(d)
                            }, 15)
                        }, 10)

                    }, (4*idx).toLong())
                }
            }
        }
    }

}