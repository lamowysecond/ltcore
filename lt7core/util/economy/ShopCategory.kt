package pl.lamas.lt7core.util.economy

import org.bukkit.Material

data class ShopCategory(
    val nameKey: String,
    val displayNameKey: String,
    val items: List<ShopItem>,
    val icon: Material,
    val slot: Int
)
