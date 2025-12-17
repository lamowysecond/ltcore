package pl.lamas.lt7core.system.economy

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.config
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Utils.addItemOrDrop
import pl.lamas.lt7core.util.Utils.getRank
import pl.lamas.lt7core.util.Utils.itemStack
import pl.lamas.lt7core.util.gui.Gui
import pl.lamas.lt7core.util.gui.GuiButton

object ActionHouseSystem {
    data class ActionHouseItem(
        val item: ItemStack,
        val price: Double,
        val seller: String
    )

    @FilePersisted("ah_items.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    var items = mutableListOf<ActionHouseItem>()

    fun addItemToActionHouse(sender: Player, price: Double) {
        if (sender.inventory.itemInMainHand.type.isAir) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "action-house.add_item.no_item_in_hand"))
            return
        }

        if (price <= 0.0) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "action-house.add_item.invalid_price"))
            return
        }

        if (items.filter { it.seller == sender.name }.size >= config.maxActionHouseItems[sender.getRank()]!!) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "action-house.add_item.too_many_items"))
            return
        }

        val item = sender.inventory.itemInMainHand.clone()
        sender.inventory.setItemInMainHand(null)
        items.addFirst(
            ActionHouseItem(
                item = item,
                price = price,
                seller = sender.name
            )
        )

        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "action-house.add_item.success", mapOf("%p" to price.toString())))
    }

    fun actionHouse(sender: Player, page: Int = 1) {
        Gui(
            "action_house_main",
            langManager.getString(sender, "action-house.gui.title"),
            6,
            sender,
            buttons = items.subList((page - 1) * 45, (page * 45).coerceAtMost(items.size)).mapIndexed { index, ahItem ->
                GuiButton(
                    slot = index % 45,
                    icon = ahItem.item.apply {
                        val meta = itemMeta.clone()
                        meta.lore = listOf(
                            langManager.getString(sender, "action-house.gui.item.lore.line1", mapOf("%s" to ahItem.price.toString())),
                            langManager.getString(sender, "action-house.gui.item.lore.line2", mapOf("%p" to ahItem.seller)),
                        )
                        itemMeta = meta
                    },
                    onClick = {
                        if (sender.name == ahItem.seller) {
                            if (!items.remove(ahItem)) {
                                actionHouse(sender, page)
                                return@GuiButton
                            }
                            sender.inventory.addItem(ahItem.item)
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "action-house.gui.item.remove_own"))
                            actionHouse(sender, page)
                            return@GuiButton
                        } else {
                            if (ahItem !in items) {
                                actionHouse(sender, page)
                                return@GuiButton
                            }
                            val balance = MoneySystem.getBalance(sender.name)
                            if (balance < ahItem.price) {
                                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "economy.not-enough-money"))
                                return@GuiButton
                            }
                            MoneySystem.removeMoney(sender.name, ahItem.price)
                            sender.addItemOrDrop(ahItem.item)
                            items.remove(ahItem)
                            MoneySystem.addMoney(ahItem.seller, ahItem.price)
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "action-house.gui.item.purchase_success", mapOf("%p" to ahItem.price.toString(), "%s" to ahItem.seller)))
                            actionHouse(sender, page)

                            Bukkit.getPlayer(ahItem.seller)?.sendMessage(LTCore.PREFIX + langManager.getString(sender, "action-house.gui.item.sold_notification", mapOf("%p" to sender.name, "%b" to ahItem.price.toString())))
                        }
                    }
                )
            }.toMutableList().apply {
                if (page > 1) {
                    add(
                        GuiButton(
                            slot = 45,
                            icon = itemStack(
                                material = org.bukkit.Material.ARROW,
                                name = langManager.getString(sender, "action-house.gui.button.previous.name"),
                                lore = listOf(langManager.getString(sender, "action-house.gui.button.previous.lore"))
                            ),
                            onClick = {
                                actionHouse(sender, page - 1)
                            }
                        )
                    )
                }
                if (items.size > page * 45) {
                    add(
                        GuiButton(
                            slot = 53,
                            icon = itemStack(
                                material = org.bukkit.Material.ARROW,
                                name = langManager.getString(sender, "action-house.gui.button.next.name"),
                                lore = listOf(langManager.getString(sender, "action-house.gui.button.next.lore"))
                            ),
                            onClick = {
                                actionHouse(sender, page + 1)
                            }
                        )
                    )
                }
                add(
                    GuiButton(
                        slot = 49,
                        icon = itemStack(
                            material = org.bukkit.Material.PAPER,
                            name = langManager.getString(sender, "action-house.your_items"),
                        ),
                        onClick = {
                            actionHousePlayerItems(sender)
                        }
                    )
                )
            }
        ).open()
    }

    fun actionHousePlayerItems(sender: Player) {
        Gui(
            "action_house_own_items",
            langManager.getString(sender, "action-house.gui.title_own_items"),
            6,
            sender,
            buttons = items.filter {it.seller == sender.name}.mapIndexed { index, ahItem ->
                GuiButton(
                    slot = index % 45,
                    icon = ahItem.item.apply {
                        itemMeta.lore = listOf(
                            langManager.getString(sender, "action-house.gui.item.lore.line1", mapOf("%p" to ahItem.price.toString())),
                            langManager.getString(sender, "action-house.gui.item.lore.line2", mapOf("%s" to ahItem.seller)),
                        )
                    },
                    onClick = {
                        if (sender.name == ahItem.seller) {
                            if (!items.remove(ahItem)) {
                                actionHousePlayerItems(sender)
                                return@GuiButton
                            }
                            sender.inventory.addItem(ahItem.item)
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "action-house.gui.item.remove_own"))
                            actionHousePlayerItems(sender)
                            return@GuiButton
                        }
                    }
                )
            }.toMutableList().apply {
                add(
                    GuiButton(
                        slot = 49,
                        icon = itemStack(
                            material = org.bukkit.Material.PAPER,
                            name = langManager.getString(sender, "action-house.main_page"),
                        ),
                        onClick = {
                            actionHouse(sender)
                        }
                    )
                )
            }
        ).open()
    }
}