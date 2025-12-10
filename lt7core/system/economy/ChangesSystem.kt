package pl.lamas.lt7core.system.economy

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.config
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.util.IgnoresSystem
import pl.lamas.lt7core.util.Utils.addItemOrDrop
import pl.lamas.lt7core.util.Utils.itemStack
import pl.lamas.lt7core.util.gui.Gui
import pl.lamas.lt7core.util.gui.GuiButton

object ChangesSystem {
    data class ChangeRequest(
        val player: Player,
        val target: Player,
        val money: Double,
        val item: ItemStack
    )

    val activeChanges: MutableList<ChangeRequest> = mutableListOf()

    fun sendChangeRequest(sender: Player, target: String, money: Double) {
        val player = Bukkit.getPlayer(target)
        if (player == null) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "economy.player-not-found", mapOf("%p" to target)))
            return
        }

        val itemInHand = sender.inventory.itemInMainHand
        if (itemInHand.type.isAir) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "changes.no_item_in_hand"))
            return
        }

        if (player.location.distance(sender.location) > config.maxChangeRequestDistance) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "changes.target_too_far", mapOf("%p" to target)))
            return
        }

        if (player.openInventory.topInventory.type == InventoryType.CRAFTING) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "changes.target_busy"))
            return
        }

        val changeRequest = ChangeRequest(
            sender,
            player,
            money,
            itemInHand.clone()
        )

        activeChanges.add(changeRequest)
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "changes.request_sent", mapOf("%p" to target)))
        if (!IgnoresSystem.isIgnoring(target, sender.name)) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "changes.request_received", mapOf("%a" to sender.name, "%i" to money.toString())))
            Gui(
                "gui_request",
                langManager.getString(player, "changes.gui_title", mapOf("%a" to sender.name)),
                3,
                player,
                listOf(
                    GuiButton(
                        13,
                        itemInHand.apply {
                            if (lore == null) {
                                lore = mutableListOf(
                                    langManager.getString(
                                        player,
                                        "changes.gui_item_cost",
                                        mapOf("%i" to money.toString())
                                    )
                                )
                                return@apply
                            }
                            lore!!.add(
                                langManager.getString(
                                    player,
                                    "changes.gui_item_cost",
                                    mapOf("%i" to money.toString())
                                )
                            )
                        }
                    ),
                    GuiButton(
                        25,
                        itemStack(
                            Material.GREEN_WOOL,
                            name = langManager.getString(player, "changes.gui_accept_button")
                        ),
                        {
                            if (MoneySystem.getBalance(player.name) < money) {
                                player.sendMessage(LTCore.PREFIX + langManager.getString(player, "changes.insufficient_funds"))
                                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "changes.request_failed_insufficient_funds", mapOf("%p" to player.name)))
                                activeChanges.removeIf { it.player.name == sender.name && it.target.name == player.name }
                                return@GuiButton
                            }
                            activeChanges.removeIf { it.player.name == sender.name && it.target.name == player.name }
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "changes.request_accepted", mapOf("%p" to player.name)))
                            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "changes.request_accepted_target", mapOf("%a" to sender.name)))

                            MoneySystem.removeMoney(sender.name, money)
                            MoneySystem.addMoney(player.name, money)

                            val itemToGive = changeRequest.item
                            player.addItemOrDrop(itemToGive)
                        }
                    ),
                    GuiButton(
                        24,
                        itemStack(
                            Material.RED_WOOL,
                            name = langManager.getString(player, "changes.gui_decline_button")
                        ),
                        {
                            activeChanges.removeIf { it.player.name == sender.name && it.target.name == player.name }
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "changes.request_declined", mapOf("%p" to player.name)))
                            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "changes.request_declined_target", mapOf("%a" to sender.name)))
                        }
                    )
                ),
                onClose = {
                    activeChanges.removeIf { it.player.name == sender.name && it.target.name == player.name }
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "changes.request_declined", mapOf("%p" to player.name)))
                    player.sendMessage(LTCore.PREFIX + langManager.getString(player, "changes.request_declined_target", mapOf("%a" to sender.name)))
                }
            ).open()
        }
    }
}