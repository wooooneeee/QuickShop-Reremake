/*
 * This file is a part of project QuickShop, the name is SuperiorSkyblock2Integration.java
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

package org.maxgamer.quickshop.integration.superiorskyblock;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.api.integration.IntegrateStage;
import org.maxgamer.quickshop.api.integration.IntegrationStage;
import org.maxgamer.quickshop.api.shop.Shop;
import org.maxgamer.quickshop.integration.AbstractQSIntegratedPlugin;
import org.maxgamer.quickshop.util.Util;
import org.maxgamer.quickshop.util.logging.container.ShopRemoveLog;
import org.maxgamer.quickshop.util.reload.ReloadResult;
import org.maxgamer.quickshop.util.reload.ReloadStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@IntegrationStage(loadStage = IntegrateStage.onEnableAfter)
public class SuperiorSkyblock2Integration extends AbstractQSIntegratedPlugin implements Listener {
    private boolean onlyOwnerCanCreateShop;
    private boolean deleteShopOnMemberLeave;
    private final List<IslandPrivilege> createNeedsPrivilege = new ArrayList<>();
    private final List<IslandPrivilege> tradeNeedsPrivilege = new ArrayList<>();
    private boolean whitelist;

    public SuperiorSkyblock2Integration(QuickShop plugin) {
        super(plugin);
        plugin.getReloadManager().register(this);
        init();
    }

    @Nullable
    private IslandPrivilege getIslandPrivilege(String name) {
        try {
            return IslandPrivilege.getByName(name);
        } catch (NullPointerException exception) {
            return null;
        }
    }

    private void init() {
        createNeedsPrivilege.clear();
        tradeNeedsPrivilege.clear();
        whitelist = plugin.getConfig().getBoolean("integration.superiorskyblock.whitelist-mode");
        onlyOwnerCanCreateShop = plugin.getConfig().getBoolean("integration.superiorskyblock.owner-create-only");
        deleteShopOnMemberLeave = plugin.getConfig().getBoolean("integration.superiorskyblock.delete-shop-on-member-leave");
        for (String s : plugin.getConfig().getStringList("integration.superiorskyblock.create-privilege-needs-list")) {
            IslandPrivilege islandPrivilege = getIslandPrivilege(s);
            if (islandPrivilege != null) {
                createNeedsPrivilege.add(islandPrivilege);
            } else {
                plugin.getLogger().log(Level.WARNING, "[SuperiorSkyblock2Integration] Invalid create island privilege: " + s);
            }
        }
        for (String s : plugin.getConfig().getStringList("integration.superiorskyblock.trade-privilege-needs-list")) {
            IslandPrivilege islandPrivilege = getIslandPrivilege(s);
            if (islandPrivilege != null) {
                tradeNeedsPrivilege.add(islandPrivilege);
            } else {
                plugin.getLogger().log(Level.WARNING, "[SuperiorSkyblock2Integration] Invalid trade island privilege: " + s);
            }
        }
        if (deleteShopOnMemberLeave) {
            registerListener();
        }
    }

    /**
     * Return the integrated plugin name.
     * For example, Residence
     *
     * @return integrated plugin
     */
    @Override
    public @NotNull String getName() {
        return "SuperiorSkyblock";
    }

    /**
     * Check if a player can create shop here
     *
     * @param player   the player want to create shop
     * @param location shop location
     * @return If you can create shop here
     */
    @Override
    public boolean canCreateShopHere(@NotNull Player player, @NotNull Location location) {
        Island island = SuperiorSkyblockAPI.getIslandAt(location);
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        if (island == null) {
            return !whitelist;
        }
        for (IslandPrivilege islandPrivilege : createNeedsPrivilege) {
            if (!island.hasPermission(superiorPlayer, islandPrivilege)) {
                return false;
            }
        }
        if (onlyOwnerCanCreateShop) {
            return island.getOwner().equals(superiorPlayer);
        } else {
            if (island.getOwner().equals(superiorPlayer)) {
                return true;
            }
            return island.isMember(superiorPlayer);
        }
    }

    /**
     * Check if a player can trade with shop here
     *
     * @param player   the player want to trade with shop
     * @param location shop location
     * @return If you can trade with shop here
     */
    @Override
    public boolean canTradeShopHere(@NotNull Player player, @NotNull Location location) {
        Island island = SuperiorSkyblockAPI.getIslandAt(location);
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        if (island == null) {
            //Allow trading outside the island area
            return true;
        }
        for (IslandPrivilege islandPrivilege : tradeNeedsPrivilege) {
            if (!island.hasPermission(superiorPlayer, islandPrivilege)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Loading logic
     * Execute Stage defined by IntegrationStage
     */
    @Override
    public void load() {
        if (deleteShopOnMemberLeave) {
            registerListener();
        }
    }

    /**
     * Unloding logic
     * Will execute when Quickshop unloading
     */
    @Override
    public void unload() {
        unregisterListener();
    }


    @EventHandler
    public void onPlayerQuitIsland(com.bgsoftware.superiorskyblock.api.events.IslandQuitEvent event) {
        for (Chunk chunk : event.getIsland().getAllChunks()) {
            Map<Location, Shop> shops = plugin.getShopManager().getShops(chunk);
            if (shops != null && !shops.isEmpty()) {
                for (Shop shop : shops.values()) {
                    if (shop.getOwner().equals(event.getPlayer().getUniqueId())) {
                        plugin.logEvent(new ShopRemoveLog(event.getPlayer().getUniqueId(), String.format("[%s Integration]Shop %s deleted caused by ShopOwnerQuitFromIsland", this.getName(), shop), shop.saveToInfoStorage()));
                        shop.delete();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerKickedFromIsland(com.bgsoftware.superiorskyblock.api.events.IslandKickEvent event) {
        for (Chunk chunk : event.getIsland().getAllChunks()) {
            Map<Location, Shop> shops = plugin.getShopManager().getShops(chunk);
            if (shops != null && !shops.isEmpty()) {
                shops.forEach((location, shop) -> {
                    if (shop.getOwner().equals(event.getTarget().getUniqueId())) {
                        plugin.logEvent(new ShopRemoveLog(event.getPlayer().getUniqueId(), String.format("[%s Integration]Shop %s deleted caused by ShopOwnerKickedFromIsland", this.getName(), shop), shop.saveToInfoStorage()));
                        shop.delete();
                    }
                });
            }
        }

    }

    @EventHandler
    public void onPlayerUnCooped(com.bgsoftware.superiorskyblock.api.events.IslandUncoopPlayerEvent event) {
        for (Chunk chunk : event.getIsland().getAllChunks()) {
            Map<Location, Shop> shops = plugin.getShopManager().getShops(chunk);
            if (shops != null && !shops.isEmpty()) {
                shops.forEach((location, shop) -> {
                    if (shop.getOwner().equals(event.getTarget().getUniqueId())) {
                        plugin.logEvent(new ShopRemoveLog(event.getPlayer().getUniqueId(), String.format("[%s Integration]Shop %s deleted caused by IslandUncoopPlayerEvent", this.getName(), shop), shop.saveToInfoStorage()));
                        shop.delete();
                    }
                });
            }
        }
    }

    @EventHandler
    public void onIslandChunkReset(com.bgsoftware.superiorskyblock.api.events.IslandChunkResetEvent event) {
        Map<Location, Shop> shops = plugin.getShopManager().getShops(event.getWorld().getName(), event.getChunkX(), event.getChunkZ());
        if (shops != null && !shops.isEmpty()) {
            for (Shop shop : shops.values()) {
                plugin.logEvent(new ShopRemoveLog(Util.getNilUniqueId(), String.format("[%s Integration]Shop %s deleted caused by IslandChunkReset", this.getName(), shop), shop.saveToInfoStorage()));
                shop.delete();
            }
        }
    }

    /**
     * Callback for reloading
     *
     * @return Reloading success
     */
    @Override
    public ReloadResult reloadModule() {
        init();
        return ReloadResult.builder().status(ReloadStatus.SUCCESS).build();
    }
}
