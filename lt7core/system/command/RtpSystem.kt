package pl.lamas.lt7core.system.command

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.config
import pl.lamas.lt7core.system.guild.GuildSystem
import pl.lamas.lt7core.util.Teleportation
import java.security.SecureRandom

object RtpSystem {
    private fun findLocation(): Location? {
        val spawnLocation = Bukkit.getWorlds().find { it.name == "world" }!!.spawnLocation
        val minx = -config.rtpRadius + spawnLocation.x
        val maxx = config.rtpRadius + spawnLocation.x
        val minz = -config.rtpRadius + spawnLocation.z
        val maxz = config.rtpRadius + spawnLocation.z
        val x = SecureRandom().nextDouble(minx, maxx)
        val z = SecureRandom().nextDouble(minz, maxz)

        val y = SecureRandom().nextDouble(55.0, 75.0)

        val location = Location(
            spawnLocation.world,
            x,
            y,
            z
        )

        if (location.block.type != Material.AIR || location.clone().add(0.0, 1.0, 0.0).block.type != Material.AIR) {
            return null
        }
        if (location.clone().add(0.0, -1.0, 0.0).block.type == Material.AIR || location.clone().add(0.0, -1.0, 0.0).block.type == Material.WATER || location.clone().add(0.0, -1.0, 0.0).block.type == Material.LAVA) {
            return null
        }

        GuildSystem.guilds.values.forEach {
            if (it.isInGuildArea(location)) {
                return null
            }
        }
        return location
    }

    fun rtp(sender: Player) {
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "rtp.searching_location"))
        var i = 0
        while (i < config.maxRtpAttempts) {
            i++
            if (i == config.maxRtpAttempts) {
                break
            }
            val location = findLocation()
            if (location != null) {
                Teleportation(
                    sender,
                    location
                ).start {
                }
                return
            }
        }
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "rtp.failed"))
    }
}