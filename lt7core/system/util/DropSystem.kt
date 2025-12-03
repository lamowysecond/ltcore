package pl.lamas.lt7core.system.util

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.config
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Utils

object DropSystem : org.bukkit.event.Listener {
    @EventHandler
    fun onDrop(event: BlockBreakEvent) {
        if (event.block.type != Material.STONE && event.block.type != Material.DEEPSLATE) return

        if (!SettingsSystem.getSetting(event.player.name, "dropCobblestone")) {
            event.isCancelled = true
            event.block.type = Material.STONE
        }

        var maxQuantity: Int = 1
        var chanceAdder: Double = 0.0

        val tool = event.player.inventory.itemInMainHand
        if (tool.containsEnchantment(Enchantment.FORTUNE)) {
            val fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE)
            maxQuantity += fortuneLevel - 1
            chanceAdder += 0.06 * fortuneLevel
        }

        for (drop in config.dropChances) {
            val settingName = when (drop.material) {
                Material.COAL -> "dropCoal"
                Material.RAW_IRON -> "dropIron"
                Material.RAW_COPPER -> "dropCopper"
                Material.RAW_GOLD -> "dropGold"
                Material.DIAMOND -> "dropDiamond"
                Material.EMERALD -> "dropEmerald"
                Material.REDSTONE -> "dropRedstone"
                Material.LAPIS_LAZULI -> "dropLapis"
                Material.NETHERITE_SCRAP -> "dropNetherite"
                else -> continue
            }

            val notificationName = when (drop.material) {
                Material.COAL -> "notifyCoal"
                Material.RAW_IRON -> "notifyIron"
                Material.RAW_COPPER -> "notifyCopper"
                Material.RAW_GOLD -> "notifyGold"
                Material.DIAMOND -> "notifyDiamond"
                Material.EMERALD -> "notifyEmerald"
                Material.REDSTONE -> "notifyRedstone"
                Material.LAPIS_LAZULI -> "notifyLapis"
                Material.NETHERITE_SCRAP -> "notifyNetherite"
                else -> continue
            }

            val percentDropped = Math.random() * 100
            val finalChance = drop.chanceByPickaxe(tool)?.plus(chanceAdder)
                ?: continue
            if (percentDropped <= finalChance && SettingsSystem.getSetting(event.player.name, settingName)) {
                val quantity = (1..maxQuantity).random()
                val item = Utils.itemStack(drop.material, quantity)
                event.block.world.dropItemNaturally(event.block.location, item)

                if (SettingsSystem.getSetting(event.player.name, notificationName)) {
                    event.player.sendMessage(
                        LTCore.PREFIX + langManager.getString(
                            event.player,
                            "drops.received_item",
                            mapOf(
                                "%item" to langManager.getString(
                                    event.player,
                                    "drops.item_name.${drop.material.name.lowercase()}"
                                ),
                                "%quantity" to quantity.toString()
                            )
                        )
                    )
                }
            }
        }
    }

    init {
        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)
    }
}