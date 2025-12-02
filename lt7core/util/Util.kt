package pl.lamas.lt7core.util

import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import pl.lamas.lt7core.util.config.ServerRank

object Utils {
    fun itemStack(
        material: Material,
        amount: Int = 1,
        name: String? = null,
        lore: List<String>? = null,
        enchantments: Map<org.bukkit.enchantments.Enchantment, Int>? = null
    ): ItemStack {
        return ItemStack(material, amount).apply {
            val meta = this.itemMeta
            if (name != null) {
                meta?.setDisplayName(name)
            }
            if (lore != null) {
                meta?.lore = lore
            }
            this.itemMeta = meta
            if (enchantments != null) {
                for ((enchantment, level) in enchantments) {
                    this.addEnchantment(enchantment, level)
                }
            }
        }
    }

    fun ItemStack.hasPersistentKey(key: String): Boolean {
        val meta = this.itemMeta ?: return false
        val namespacedKey = org.bukkit.NamespacedKey(pl.lamas.lt7core.LTCore.instance, key)
        return meta.persistentDataContainer.has(namespacedKey, org.bukkit.persistence.PersistentDataType.STRING)
    }

    fun ItemStack.persistentDataGetString(key: String): String? {
        val meta = this.itemMeta ?: return null
        val namespacedKey = org.bukkit.NamespacedKey(pl.lamas.lt7core.LTCore.instance, key)
        return meta.persistentDataContainer.get(namespacedKey, org.bukkit.persistence.PersistentDataType.STRING)
    }

    fun Player.getRank(): ServerRank {
        return when {
            this.hasPermission("group.utopian") -> ServerRank.UTOPIEC
            this.hasPermission("group.chrumka") -> ServerRank.CHRUMKA
            this.hasPermission("group.lama") -> ServerRank.LAMA
            else -> ServerRank.DEFAULT
        }
    }

    fun Player.addItemOrDrop(item: ItemStack) {
        val leftover = this.inventory.addItem(item)
        for (left in leftover.values) {
            this.world.dropItemNaturally(this.location, left)
        }
    }

    fun Player.countExactItem(itemToCheck: ItemStack): Int {
        return inventory.contents.filterNotNull()
            .filter { invItem ->
                invItem.type == itemToCheck.type &&
                        invItem.itemMeta?.displayName == itemToCheck.itemMeta?.displayName &&
                        invItem.itemMeta?.lore == itemToCheck.itemMeta?.lore
            }
            .sumOf { it.amount } // sumujemy ilości ze wszystkich stacków
    }

    fun Player.hasAtLeast(itemToCheck: ItemStack, amount: Int): Boolean {
        return countExactItem(itemToCheck) >= amount
    }

    fun Player.hasExactItem(itemToCheck: ItemStack): Boolean {
        return inventory.contents.any { invItem ->
            invItem != null &&
                    invItem.type == itemToCheck.type &&
                    invItem.itemMeta?.displayName == itemToCheck.itemMeta?.displayName &&
                    invItem.itemMeta?.lore == itemToCheck.itemMeta?.lore
        }
    }

    fun broadcast(messageKey: String, placeholders: Map<String, String> = emptyMap()) {
        pl.lamas.lt7core.LTCore.instance.server.onlinePlayers.forEach { player ->
            val message = pl.lamas.lt7core.LTCore.langManager.getString(player, messageKey, placeholders)
            player.sendMessage(pl.lamas.lt7core.LTCore.PREFIX + message)
        }
    }
}