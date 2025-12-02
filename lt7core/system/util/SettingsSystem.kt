package pl.lamas.lt7core.system.util

import org.bukkit.Material
import org.bukkit.entity.Player
import org.lamas.lt7core.util.filesystem.FilePersisted
import org.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Utils.itemStack
import pl.lamas.lt7core.util.gui.Gui
import pl.lamas.lt7core.util.gui.GuiButton

object SettingsSystem {
    data class Settings(
        val showCoordsToFriends: Boolean = true,
        val showOnlineTimeToFriends: Boolean = true,
        val autoAcceptTpaRequestFromFriends: Boolean = false,
        val dropCobblestone: Boolean = true,
        val dropCoal: Boolean = true,
        val dropIron: Boolean = true,
        val dropGold: Boolean = true,
        val dropCopper: Boolean = true,
        val dropDiamond: Boolean = true,
        val dropEmerald: Boolean = true,
        val dropLapis: Boolean = true,
        val dropRedstone: Boolean = true,
        val dropNetherite: Boolean = true,
        val notifyCoal: Boolean = true,
        val notifyIron: Boolean = true,
        val notifyGold: Boolean = true,
        val notifyCopper: Boolean = true,
        val notifyDiamond: Boolean = true,
        val notifyEmerald: Boolean = true,
        val notifyLapis: Boolean = true,
        val notifyRedstone: Boolean = true,
        val notifyNetherite: Boolean = true
    )

    val iconsForSettings: Map<String, Material> = mapOf(
        "showCoordsToFriends" to Material.COMPASS,
        "showOnlineTimeToFriends" to Material.CLOCK,
        "autoAcceptTpaRequestFromFriends" to Material.ENDER_PEARL,
        "dropCobblestone" to Material.COBBLESTONE,
        "dropCoal" to Material.COAL,
        "dropIron" to Material.IRON_INGOT,
        "dropGold" to Material.GOLD_INGOT,
        "dropCopper" to Material.COPPER_INGOT,
        "dropDiamond" to Material.DIAMOND,
        "dropEmerald" to Material.EMERALD,
        "dropLapis" to Material.LAPIS_LAZULI,
        "dropRedstone" to Material.REDSTONE,
        "dropNetherite" to Material.NETHERITE_SCRAP,
        "notifyCoal" to Material.COAL_ORE,
        "notifyIron" to Material.IRON_ORE,
        "notifyGold" to Material.GOLD_ORE,
        "notifyCopper" to Material.COPPER_ORE,
        "notifyDiamond" to Material.DIAMOND_ORE,
        "notifyEmerald" to Material.EMERALD_ORE,
        "notifyLapis" to Material.LAPIS_ORE,
        "notifyRedstone" to Material.REDSTONE_ORE,
        "notifyNetherite" to Material.ANCIENT_DEBRIS
    )

    @FilePersisted("settings.json", PersistType.READ_SAVE, 60L)
    val settings: MutableMap<String, Settings> = mutableMapOf()

    fun getSetting(playerName: String, settingKey: String): Boolean {
        val playerSettings = settings.getOrDefault(playerName, Settings())
        return playerSettings.javaClass.getDeclaredField(settingKey).getBoolean(playerSettings)
    }

    fun settingsGui(sender: Player) {
        val playerSettings = settings.getOrDefault(sender.name, Settings())
        Gui(
            "settings",
            langManager.getString(sender, "settings.title"),
            6,
            sender,
            iconsForSettings.map {
                val x = iconsForSettings.keys.indexOf(it.key)
                val isOn = playerSettings.javaClass.getDeclaredField(it.key).getBoolean(playerSettings)
                GuiButton(
                    x + 10 + 3 * (x / 7),
                    itemStack(
                        it.value,
                        1,
                        langManager.getString(sender, "settings.${it.key}.name"),
                        lore = listOf(langManager.getString(sender, if (isOn) "settings.enabled" else "settings.disabled"))
                    ),
                    onClick = { event ->
                        val currentSettings = settings.getOrDefault(sender.name, Settings())
                        val field = currentSettings.javaClass.getDeclaredField(it.key)
                        val newValue = !field.getBoolean(currentSettings)
                        val newSettings = currentSettings.copy().apply {
                            this.javaClass.getDeclaredField(it.key).setBoolean(this, newValue)
                        }
                        settings[sender.name] = newSettings
                        settingsGui(sender)
                    }
                )
            }
        )
    }
}