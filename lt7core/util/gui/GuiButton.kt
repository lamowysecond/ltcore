package pl.lamas.lt7core.util.gui

data class GuiButton(
    val slot: Int,
    val icon: org.bukkit.inventory.ItemStack,
    val onClick: ((event: org.bukkit.event.inventory.InventoryClickEvent) -> Unit)? = null
)
