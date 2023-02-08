package com.hanwoo.playground.misc

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.util.Vector

object Emote {
    val emotes = mutableMapOf<List<String>, (Location) -> Unit>()

    private fun register(names: List<String>, emote: (Location) -> Unit) {
        emotes[names] = emote
    }

    fun getEmote(name: String): ((Location) -> Unit)? {
        return emotes.filter { it.key.contains(name.lowercase()) }.map { it.value }.firstOrNull()
    }

    private fun Location.headParticle(particle: Particle) {
        this.add(0.0, 2.0, 0.0).spreadParticle(particle)
    }

    private fun Location.spreadParticle(particle: Particle, count: Int =1, spread: Double =0.0, extra: Double = 0.0) {
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
        register(listOf("angry", "grrr")) {
            it.headParticle(Particle.VILLAGER_ANGRY)
        }
        register(listOf("love", "heart")) {
            it.headParticle(Particle.HEART)
        }
        register(listOf("damage", "ouch")) {
            it.headParticle(Particle.DAMAGE_INDICATOR)
        }
        register(listOf("critical", "puk")) {
            it.spreadParticle(Particle.DAMAGE_INDICATOR, 16, 0.5)
        }
        register(listOf("spit")) {
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
        register(listOf("no", "ban")) {
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
        register(listOf("note", "music")) {
            it.headParticle(Particle.NOTE)
        }
        register(listOf("rage", "grrrrr")) {
            it.spreadParticle(Particle.LAVA, 50)
            it.world.playSound(it, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F)
        }
        register(listOf("tear", "sob", "sad")) {
            for (i in 0..1) {
                val v = Vector(-0.1 + i * 0.2, 0.1, 0.4)
                v.rotateAroundX(Math.toRadians(it.pitch.toDouble())).rotateAroundY(Math.toRadians(-it.yaw.toDouble()))
                it.clone().add(v.x, 1.3 + v.y, v.z).spreadParticle(Particle.FALLING_WATER, 1, 0.0, 1.0)
            }
        }
    }

}