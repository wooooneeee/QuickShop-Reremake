/*
 * This file is a part of project QuickShop, the name is EnhanceDisplayProtectionListener.java
 *  Copyright (C) PotatoCraft Studio and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.maxgamer.quickshop.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.Cache;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.shop.DisplayItem;
import org.maxgamer.quickshop.shop.DisplayType;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.util.MsgUtil;
import org.maxgamer.quickshop.util.Util;

public class EnhanceDisplayProtectionListener extends ProtectionListenerBase implements Listener {

    public EnhanceDisplayProtectionListener(QuickShop plugin, Cache cache) {
        super(plugin, cache);
    }

    private void sendAlert(@NotNull String msg) {
        if (!plugin.getConfig().getBoolean("send-display-item-protection-alert")) {
            return;
        }
        MsgUtil.sendGlobalAlert(msg);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void block(BlockFromToEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        final Block targetBlock = event.getToBlock();
        final Block shopBlock = targetBlock.getRelative(BlockFace.DOWN);
        final Shop shop = getShopNature(shopBlock.getLocation(), true);
        if (shop == null) {
            return;
        }
        event.setCancelled(true);
        if (shop.getDisplay() != null) {
            shop.getDisplay().remove();
        }
        sendAlert(
                "[DisplayGuard] Liuqid "
                        + targetBlock.getLocation()
                        + " trying flow to top of shop, QuickShop already cancel it.");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void block(BlockPistonExtendEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        final Block block = event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.DOWN);
        Shop shop = getShopRedstone(block.getLocation(), true);
        if (shop != null) {
            event.setCancelled(true);
            sendAlert(
                    "[DisplayGuard] Piston  "
                            + event.getBlock().getLocation()
                            + " trying push somethings on the shop top, QuickShop already cancel it.");
            if (shop.getDisplay() != null) {
                shop.getDisplay().remove();
            }
            return;
        }
        for (Block oBlock : event.getBlocks()) {
            final Block otherBlock = oBlock.getRelative(event.getDirection()).getRelative(BlockFace.DOWN);
            if (Util.canBeShop(otherBlock)) {
                shop = getShopNature(otherBlock.getLocation(), true);
                if (shop != null) {
                    event.setCancelled(true);
                    sendAlert(
                            "[DisplayGuard] Piston  "
                                    + event.getBlock().getLocation()
                                    + " trying push somethings on the shop top, QuickShop already cancel it.");
                    if (shop.getDisplay() != null) {
                        shop.getDisplay().remove();
                    }
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void block(BlockPistonRetractEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        final Block block = event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.DOWN);
        Shop shop = getShopNature(block.getLocation(), true);
        if (shop != null) {
            event.setCancelled(true);
            sendAlert(
                    "[DisplayGuard] Piston  "
                            + event.getBlock().getLocation()
                            + " trying pull somethings on the shop top, QuickShop already cancel it.");
            if (shop.getDisplay() != null) {
                shop.getDisplay().remove();
            }
            return;
        }
        for (Block oBlock : event.getBlocks()) {
            final Block otherBlock = oBlock.getRelative(event.getDirection()).getRelative(BlockFace.DOWN);
            if (Util.canBeShop(otherBlock)) {
                shop = getShopNature(otherBlock.getLocation(), true);
                if (shop != null) {
                    event.setCancelled(true);
                    sendAlert(
                            "[DisplayGuard] Piston  "
                                    + event.getBlock().getLocation()
                                    + " trying push somethings on the shop top, QuickShop already cancel it.");
                    if (shop.getDisplay() != null) {
                        shop.getDisplay().remove();
                    }
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void block(BrewingStandFuelEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        final ItemStack itemStack = event.getFuel();
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
            sendAlert(
                    "[DisplayGuard] Block  "
                            + event.getBlock().getLocation()
                            + " trying fuel the BrewingStand with DisplayItem.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void block(FurnaceBurnEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        final ItemStack itemStack = event.getFuel();
        BlockState state = plugin.getPerformanceUtil().getState(event.getBlock());
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
            Block furnace = event.getBlock();
            if (state instanceof Furnace) {
                Furnace furnace1 = (Furnace) furnace.getState();
                sendAlert("[DisplayGuard] Block  " + event.getBlock().getLocation() + " trying burn with DisplayItem.");
                Util.inventoryCheck(furnace1.getInventory());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void block(FurnaceSmeltEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        ItemStack itemStack = event.getSource();
        BlockState furnace = plugin.getPerformanceUtil().getState(event.getBlock());
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
            if (furnace instanceof Furnace) {
                Furnace furnace1 = (Furnace) furnace;
                sendAlert(
                        "[DisplayGuard] Block  "
                                + event.getBlock().getLocation()
                                + " trying smelt with DisplayItem.");
                Util.inventoryCheck(furnace1.getInventory());
            }
            return;
        }
        itemStack = event.getResult();
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
            if (furnace instanceof Furnace) {
                Furnace furnace1 = (Furnace) furnace;
                sendAlert(
                        "[DisplayGuard] Block  "
                                + event.getBlock().getLocation()
                                + " trying smelt with DisplayItem.");
                Util.inventoryCheck(furnace1.getInventory());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void entity(EntityPickupItemEvent e) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        final ItemStack stack = e.getItem().getItemStack();
        if (!DisplayItem.checkIsGuardItemStack(stack)) {
            return;
        }
        e.setCancelled(true);
        // You shouldn't be able to pick up that...
        e.getItem().remove();
        sendAlert(
                "[DisplayGuard] Entity "
                        + e.getEntity().getName()
                        + " # "
                        + e.getEntity().getLocation()
                        + " pickedup the displayItem, QuickShop already removed it.");

        Entity entity = e.getEntity();
        if (entity instanceof InventoryHolder) {
            Util.inventoryCheck(((InventoryHolder) entity).getInventory());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void entity(EntityDeathEvent event) {
        if (DisplayItem.getNowUsing() != DisplayType.ARMORSTAND) {
            return;
        }
        if (!(event.getEntity() instanceof ArmorStand)) {
            return;
        }
        if (!DisplayItem.checkIsGuardItemStack(((ArmorStand) event.getEntity()).getItemInHand())) { //FIXME: Update this when drop 1.13 supports
            return;
        }
        event.setDroppedExp(0);
        event.getDrops().clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void entity(EntityInteractEvent event) {
        if (DisplayItem.getNowUsing() != DisplayType.ARMORSTAND) {
            return;
        }
        if (!(event.getEntity() instanceof ArmorStand)) {
            return;
        }
        if (!DisplayItem.checkIsGuardItemStack(((ArmorStand) event.getEntity()).getItemInHand())) { //FIXME: Update this when drop 1.13 supports
            return;
        }
        event.setCancelled(true);
        final Entity entity = event.getEntity();
        if (entity instanceof InventoryHolder) {
            Util.inventoryCheck(((InventoryHolder) entity).getInventory());
        }
        sendAlert(
                "[DisplayGuard] Entity  "
                        + event.getEntityType().name()
                        + " # "
                        + event.getEntity().getLocation()
                        + " trying interact the hold displayItem's entity.");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void inventory(InventoryClickEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        if (!DisplayItem.checkIsGuardItemStack(event.getCurrentItem())) {
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory().getLocation() == null) {
            return;
        }
        event.setCancelled(true);

        sendAlert(
                "[DisplayGuard] Inventory "
                        + event.getClickedInventory().getHolder()
                        + " at"
                        + event.getClickedInventory().getLocation()
                        + " was clicked the displayItem, QuickShop already removed it.");
        //event.getCurrentItem().setAmount(0);
        event.getCurrentItem().setType(Material.AIR);
        event.setResult(Event.Result.DENY);
        Util.inventoryCheck(event.getInventory());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void inventory(InventoryDragEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        ItemStack itemStack = event.getCursor();
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
            Util.inventoryCheck(event.getInventory());
            sendAlert(
                    "[DisplayGuard] Player  "
                            + event.getWhoClicked().getName()
                            + " trying use DisplayItem crafting.");
            return;
        }
        itemStack = event.getOldCursor();
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
            Util.inventoryCheck(event.getInventory());
            sendAlert(
                    "[DisplayGuard] Player  "
                            + event.getWhoClicked().getName()
                            + " trying use DisplayItem crafting.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void inventory(InventoryCreativeEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        ItemStack itemStack = event.getCursor();
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
            Util.inventoryCheck(event.getInventory());
            sendAlert(
                    "[DisplayGuard] Player  "
                            + event.getWhoClicked().getName()
                            + " trying use DisplayItem crafting.");
            return;
        }
        itemStack = event.getCurrentItem();
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
            Util.inventoryCheck(event.getInventory());
            sendAlert(
                    "[DisplayGuard] Player  "
                            + event.getWhoClicked().getName()
                            + " trying use DisplayItem crafting.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void item(PlayerItemHeldEvent e) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        final ItemStack stack = e.getPlayer().getInventory().getItemInMainHand();
        final ItemStack stackOffHand = e.getPlayer().getInventory().getItemInOffHand();
        if (DisplayItem.checkIsGuardItemStack(stack)) {
            e.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR, 0));
            // You shouldn't be able to pick up that...
            sendAlert(
                    "[DisplayGuard] Player "
                            + e.getPlayer().getName()
                            + " helded the displayItem, QuickShop already cancelled and removed it.");
            e.setCancelled(true);
            Util.inventoryCheck(e.getPlayer().getInventory());
        }
        if (DisplayItem.checkIsGuardItemStack(stackOffHand)) {
            e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.AIR, 0));
            // You shouldn't be able to pick up that...
            sendAlert(
                    "[DisplayGuard] Player "
                            + e.getPlayer().getName()
                            + " helded the displayItem, QuickShop already cancelled and removed it.");
            e.setCancelled(true);
            Util.inventoryCheck(e.getPlayer().getInventory());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void player(CraftItemEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        ItemStack itemStack;
        itemStack = event.getCurrentItem();
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
            Util.inventoryCheck(event.getInventory());
            sendAlert(
                    "[DisplayGuard] Player  "
                            + event.getWhoClicked().getName()
                            + " trying use DisplayItem crafting.");
            return;
        }
        itemStack = event.getCursor();
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
            Util.inventoryCheck(event.getInventory());
            sendAlert(
                    "[DisplayGuard] Player  "
                            + event.getWhoClicked().getName()
                            + " trying use DisplayItem crafting.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void player(PlayerBucketEmptyEvent event) {
//        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
//            return;
//        }
        final Block waterBlock = event.getBlockClicked().getRelative(event.getBlockFace());
        final Shop shop =
                getShopPlayer(waterBlock.getRelative(BlockFace.DOWN).getLocation(), true);
        if (shop == null) {
            return;
        }
        event.setCancelled(true);
        sendAlert(
                "[DisplayGuard] Player  "
                        + event.getPlayer().getName()
                        + " trying use water to move somethings on the shop top, QuickShop already remove it.");
    }
}
