package pl.lamas.lt7core.util.drop

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class DropChances(
    val material: Material,
    val chancesWooden: Double? = 0.0,
    val chancesGolden: Double? = 0.0,
    val chancesStone: Double? = 0.0,
    val chancesCopper: Double? = 0.0,
    val chancesIron: Double? = 0.0,
    val chancesDiamond: Double? = 0.0,
    val chancesNetherite: Double? = 0.0
) {
    fun chanceByPickaxe(item: ItemStack): Double? {
        return when (item.type) {
            Material.WOODEN_PICKAXE -> chancesWooden
            Material.GOLDEN_PICKAXE -> chancesGolden
            Material.STONE_PICKAXE -> chancesStone
            //Material.COPPER_PICKAXE -> chancesCopper
            Material.IRON_PICKAXE -> chancesIron
            Material.DIAMOND_PICKAXE -> chancesDiamond
            Material.NETHERITE_PICKAXE -> chancesNetherite
            else -> null
        }
    }
}