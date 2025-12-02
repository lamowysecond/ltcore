package pl.lamas.lt7core.util.economy

import org.bukkit.inventory.ItemStack

data class ShopItem(
    val item: ItemStack,
    val buyPrice: Double,
    val sellPrice: Double
)
