/*
 * This file is a part of project QuickShop, the name is LandsIntegration.java
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

package org.maxgamer.quickshop.integration.lands;

import me.angeschossen.lands.api.events.LandDeleteEvent;
import me.angeschossen.lands.api.events.LandUntrustPlayerEvent;
import me.angeschossen.lands.api.events.PlayerLeaveLandEvent;
import me.angeschossen.lands.api.events.land.DeleteReason;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.api.integration.IntegrationStage;
import org.maxgamer.quickshop.api.shop.Shop;
import org.maxgamer.quickshop.api.shop.ShopChunk;
import org.maxgamer.quickshop.integration.AbstractQSIntegratedPlugin;
import org.maxgamer.quickshop.util.Util;
import org.maxgamer.quickshop.util.logging.container.ShopRemoveLog;
import org.maxgamer.quickshop.util.reload.ReloadResult;
import org.maxgamer.quickshop.util.reload.ReloadStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@IntegrationStage
public class LandsIntegration extends AbstractQSIntegratedPlugin implements Listener {

    private boolean ignoreDisabledWorlds;
    private boolean whitelist;
    private me.angeschossen.lands.api.integration.LandsIntegration landsIntegration;
    private boolean deleteWhenLosePermission;
    private boolean deleteWhenLandDeleted;
    private boolean deleteWhenLandExpired;

    public LandsIntegration(QuickShop plugin) {
        super(plugin);
        plugin.getReloadManager().register(this);
        init();
    }

    private void init() {
        landsIntegration = new me.angeschossen.lands.api.integration.LandsIntegration(plugin);
        ignoreDisabledWorlds = plugin.getConfig().getBoolean("integration.lands.ignore-disabled-worlds");
        whitelist = plugin.getConfig().getBoolean("integration.lands.whitelist-mode");
        deleteWhenLosePermission = plugin.getConfig().getBoolean("integration.lands.delete-on-lose-permission");
        deleteWhenLandDeleted = plugin.getConfig().getBoolean("integration.lands.delete-on-land-deleted");
        deleteWhenLandExpired = plugin.getConfig().getBoolean("integration.lands.delete-on-land-expired");
    }

    @Override
    public @NotNull String getName() {
        return "Lands";
    }

    @Override
    public boolean canCreateShopHere(@NotNull Player player, @NotNull Location location) {
        if (landsIntegration.getLandWorld(location.getWorld()) == null) {
            return ignoreDisabledWorlds;
        }
        org.bukkit.Chunk chunk = location.getChunk();
        Land land = landsIntegration.getLand(chunk.getWorld(), chunk.getX(), chunk.getZ());
        if (land != null) {
            return land.getOwnerUID().equals(player.getUniqueId()) || land.isTrusted(player.getUniqueId());
        } else {
            return !whitelist;
        }
    }

    @Override
    public boolean canTradeShopHere(@NotNull Player player, @NotNull Location location) {
        if (landsIntegration.getLandWorld(location.getWorld()) == null) {
            return ignoreDisabledWorlds;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandsDelete(LandDeleteEvent event) {
        @NotNull DeleteReason reason = event.getReason();
        if (reason != me.angeschossen.lands.api.events.land.DeleteReason.CAMP_EXPIRED) {
            if (!deleteWhenLandDeleted) {
                return;
            }
        } else if (!deleteWhenLandExpired) {
            return;
        }
        Land land = event.getLand();
        Set<UUID> uuids = new HashSet<>(land.getTrustedPlayers());
        uuids.add(land.getOwnerUID());
        deleteShopInLand(event.getLand(), uuids, reason == DeleteReason.CAMP_EXPIRED ? Reason.EXPIRED : Reason.DELETED);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandsPermissionChanges(LandUntrustPlayerEvent event) {
        if (!deleteWhenLosePermission) {
            return;
        }
        deleteShopInLand(event.getLand(), event.getTargetUID(), Reason.UNTRUSTED);
    }

    private void deleteShopInLand(Land land, UUID target, Reason reason) {
        deleteShopInLand(land, Collections.singleton(target), reason);
    }

    private void deleteShopInLand(Land land, Collection<UUID> targets, Reason reason) {
        final Collection<UUID> finalTargets = new CopyOnWriteArrayList<>(targets);
        //Getting all shop with world-chunk-shop mapping
        for (Map.Entry<String, Map<ShopChunk, Map<Location, Shop>>> entry : plugin.getShopManager().getShops().entrySet()) {
            //Matching world
            World world = plugin.getServer().getWorld(entry.getKey());
            if (world != null) {
                //Matching chunk
                for (Map.Entry<ShopChunk, Map<Location, Shop>> chunkedShopEntry : entry.getValue().entrySet()) {
                    ShopChunk shopChunk = chunkedShopEntry.getKey();
                    if (land.hasChunk(world, shopChunk.getX(), shopChunk.getZ())) {
                        //Matching Owner and delete it
                        Map<Location, Shop> shops = chunkedShopEntry.getValue();
                        List<Shop> shopToRemove = new CopyOnWriteArrayList<>(shops.values()).parallelStream().filter(shop -> finalTargets.contains(shop.getOwner())).collect(Collectors.toList());
                        Util.mainThreadRun(() -> {
                            final String reasonStr = "[Lands Integration] " + reason.message;
                            for (Shop shop : shopToRemove) {
                                plugin.logEvent(new ShopRemoveLog(Util.getNilUniqueId(), reasonStr, shop.saveToInfoStorage()));
                                shop.delete();
                            }
                        });
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandsMember(PlayerLeaveLandEvent event) {
        if (!deleteWhenLosePermission) {
            return;
        }
        deleteShopInLand(event.getLand(), event.getLandPlayer().getUID(), Reason.LEAVED);
    }

    private enum Reason {
        UNTRUSTED("Land member was untrusted"),
        LEAVED("Land member was leave"),
        DELETED("Land was deleted"),
        EXPIRED("Land was expired");
        final String message;

        Reason(String message) {
            this.message = message;
        }
    }

    @Override
    public void load() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public ReloadResult reloadModule() {
        init();
        return ReloadResult.builder().status(ReloadStatus.SUCCESS).build();
    }
}
