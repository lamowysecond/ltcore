package pl.lamas.lt7core.util.economy

import org.bukkit.Material

data class ShopItem(
    val item: Material,
    val buyPrice: Double,
    val sellPrice: Double
)
