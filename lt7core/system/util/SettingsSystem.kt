package pl.lamas.lt7core.system.util

import org.bukkit.Material
import org.bukkit.entity.Player
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Utils.itemStack
import pl.lamas.lt7core.util.gui.Gui
import pl.lamas.lt7core.util.gui.GuiButton
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object SettingsSystem {
    data class Settings(
        var showCoordsToFriends: Boolean = true,
        var showOnlineTimeToFriends: Boolean = true,
        var autoAcceptTpaRequestFromFriends: Boolean = false,
        var dropCobblestone: Boolean = true,
        var dropCoal: Boolean = true,
        var dropIron: Boolean = true,
        var dropGold: Boolean = true,
        var dropCopper: Boolean = true,
        var dropDiamond: Boolean = true,
        var dropEmerald: Boolean = true,
        var dropLapis: Boolean = true,
        var dropRedstone: Boolean = true,
        var dropNetherite: Boolean = true,
        var notifyCoal: Boolean = true,
        var notifyIron: Boolean = true,
        var notifyGold: Boolean = true,
        var notifyCopper: Boolean = true,
        var notifyDiamond: Boolean = true,
        var notifyEmerald: Boolean = true,
        var notifyLapis: Boolean = true,
        var notifyRedstone: Boolean = true,
        var notifyNetherite: Boolean = true
    ) {
        fun get(settingKey: String): Boolean {
            return when (settingKey) {
                "showCoordsToFriends" -> showCoordsToFriends
                "showOnlineTimeToFriends" -> showOnlineTimeToFriends
                "autoAcceptTpaRequestFromFriends" -> autoAcceptTpaRequestFromFriends
                "dropCobblestone" -> dropCobblestone
                "dropCoal" -> dropCoal
                "dropIron" -> dropIron
                "dropGold" -> dropGold
                "dropCopper" -> dropCopper
                "dropDiamond" -> dropDiamond
                "dropEmerald" -> dropEmerald
                "dropLapis" -> dropLapis
                "dropRedstone" -> dropRedstone
                "dropNetherite" -> dropNetherite
                "notifyCoal" -> notifyCoal
                "notifyIron" -> notifyIron
                "notifyGold" -> notifyGold
                "notifyCopper" -> notifyCopper
                "notifyDiamond" -> notifyDiamond
                "notifyEmerald" -> notifyEmerald
                "notifyLapis" -> notifyLapis
                "notifyRedstone" -> notifyRedstone
                "notifyNetherite" -> notifyNetherite
                else -> error("Unknown setting: $settingKey")
            }
        }

        fun toggle(settingKey: String) {
            when (settingKey) {
                "showCoordsToFriends" -> showCoordsToFriends = !showCoordsToFriends
                "showOnlineTimeToFriends" -> showOnlineTimeToFriends = !showOnlineTimeToFriends
                "autoAcceptTpaRequestFromFriends" -> autoAcceptTpaRequestFromFriends = !autoAcceptTpaRequestFromFriends
                "dropCobblestone" -> dropCobblestone = !dropCobblestone
                "dropCoal" -> dropCoal = !dropCoal
                "dropIron" -> dropIron = !dropIron
                "dropGold" -> dropGold = !dropGold
                "dropCopper" -> dropCopper = !dropCopper
                "dropDiamond" -> dropDiamond = !dropDiamond
                "dropEmerald" -> dropEmerald = !dropEmerald
                "dropLapis" -> dropLapis = !dropLapis
                "dropRedstone" -> dropRedstone = !dropRedstone
                "dropNetherite" -> dropNetherite = !dropNetherite
                "notifyCoal" -> notifyCoal = !notifyCoal
                "notifyIron" -> notifyIron = !notifyIron
                "notifyGold" -> notifyGold = !notifyGold
                "notifyCopper" -> notifyCopper = !notifyCopper
                "notifyDiamond" -> notifyDiamond = !notifyDiamond
                "notifyEmerald" -> notifyEmerald = !notifyEmerald
                "notifyLapis" -> notifyLapis = !notifyLapis
                "notifyRedstone" -> notifyRedstone = !notifyRedstone
                "notifyNetherite" -> notifyNetherite = !notifyNetherite
                else -> error("Unknown setting: $settingKey")
            }
        }
    }

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
    var settings: MutableMap<String, Settings> = mutableMapOf()

    fun getSetting(playerName: String, settingKey: String): Boolean {
        val playerSettings = settings.getOrDefault(playerName, Settings())
        return playerSettings.get(settingKey)
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
                val isOn = getSetting(sender.name, it.key)
                GuiButton(
                    x + 10 + 2 * (x / 7),
                    itemStack(
                        it.value,
                        1,
                        langManager.getString(sender, "settings.${it.key}.name"),
                        lore = listOf(langManager.getString(sender, if (isOn) "settings.enabled" else "settings.disabled"))
                    ),
                    onClick = { event ->
                        settings[sender.name] = playerSettings.apply {
                            toggle(it.key)
                        }
                        settingsGui(sender)
                    }
                )
            }
        ).open()
    }
}