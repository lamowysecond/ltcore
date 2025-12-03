package pl.lamas.lt7core.util.gui

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import pl.lamas.lt7core.LTCore

data class Gui(
    val id: String,
    val title: String,
    val rows: Int,
    val holder: InventoryHolder?,
    val buttons: List<GuiButton> = listOf(),
    val cancelInteractions: Boolean = true,
    val onClick: ((event: org.bukkit.event.inventory.InventoryClickEvent) -> Unit)? = null,
    val onClose: ((event: org.bukkit.event.inventory.InventoryCloseEvent) -> Unit)? = null,
    val onOpen: ((event: org.bukkit.event.inventory.InventoryOpenEvent) -> Unit)? = null,
    val onDrag: ((event: org.bukkit.event.inventory.InventoryDragEvent) -> Unit)? = null
) : Listener {
    val inventory: Inventory = Bukkit.createInventory(holder, rows * 9, title)

    fun open() {
        holder?.let {
            if (it is org.bukkit.entity.Player) {
                it.openInventory(inventory)
            }
        }
    }

    @EventHandler
    private fun onClick(event: org.bukkit.event.inventory.InventoryClickEvent) {
        if (cancelInteractions) {
            event.isCancelled = true
        }
        if (event.inventory != inventory) {
            return
        }

        if (event.slot in buttons.map { it.slot }) {
            val button = buttons.first { it.slot == event.slot }
            if (event.currentItem != null && event.currentItem == button.icon)
            button.onClick?.invoke(event)
        }

        onClick?.invoke(event)
    }

    @EventHandler
    private fun onClose(event: org.bukkit.event.inventory.InventoryCloseEvent) {
        onClose?.invoke(event)
    }

    @EventHandler
    private fun onOpen(event: org.bukkit.event.inventory.InventoryOpenEvent) {
        onOpen?.invoke(event)
    }

    @EventHandler
    private fun onDrag(event: org.bukkit.event.inventory.InventoryDragEvent) {
        if (cancelInteractions) {
            event.isCancelled = true
        }
        if (event.inventory != inventory) {
            return
        }
        onDrag?.invoke(event)
    }

    init {
        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)

        for (button in buttons) {
            inventory.setItem(button.slot, button.icon)
        }
    }
}