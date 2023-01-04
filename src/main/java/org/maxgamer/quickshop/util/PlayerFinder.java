/*
 * This file is a part of project QuickShop, the name is PlayerFinder.java
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

package org.maxgamer.quickshop.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A player finder for finding player by name
 *
 * @author sandtechnology
 * @since 5.1.0.3
 */
public final class PlayerFinder {

    //Hold store for large server
    private static final Map<String, UUID> string2UUIDStash = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Cache<String, UUID> string2UUIDCache = CacheBuilder.newBuilder().expireAfterAccess(60, TimeUnit.MINUTES).build();

    private static volatile boolean useOfflineStash;

    private PlayerFinder() {
    }

    public static UUID findUUIDByName(String name) {
        return findOfflinePlayerByName(name).getUniqueId();
    }

    @Nullable
    private static OfflinePlayer findPlayerByName(String name, java.util.Collection<? extends org.bukkit.OfflinePlayer> players, boolean isOfflinePlayer) {
        for (OfflinePlayer player : players) {
            String playerName = player.getName();
            if (playerName != null) {
                if (playerName.equalsIgnoreCase(name)) {
                    return player;
                }
            }
        }
        return null;
    }

    public static Set<String> getCachedOfflinePlayerNames() {
        return string2UUIDStash.keySet();
    }

    public static void updateStashIfNeeded(Player player) {
        if (useOfflineStash) {
            string2UUIDStash.put(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());
        }
    }

    public static void doLargeOfflineCachingWork(QuickShop quickShop, OfflinePlayer[] offlinePlayers) {
        quickShop.getLogger().log(Level.INFO, "Large server detected (offline player > 2000), start offline player caching...");
        useOfflineStash = true;
        int amount = 0;
        int errorAmount = 0;
        int doneAmount = 0;
        for (OfflinePlayer offlinePlayer : quickShop.getServer().getOfflinePlayers()) {
            try {
                String name = offlinePlayer.getName();
                if (name != null) {
                    string2UUIDStash.put(name, offlinePlayer.getUniqueId());
                }
            } catch (Throwable ignored) {
                errorAmount++;
            }
            amount++;
            if (amount == 1000) {
                doneAmount += 1000;
                amount = 0;
                quickShop.getLogger().log(Level.INFO, "Caching Offline player...cached " + doneAmount + "/" + offlinePlayers.length + " players, " + errorAmount + " errors got when caching players.");
            }
        }
        quickShop.getLogger().log(Level.INFO, "Done! cached " + offlinePlayers.length + " players, " + errorAmount + " errors got when caching players.");
    }

    public static OfflinePlayer findOfflinePlayerByName(String name) {
        OfflinePlayer result;
        UUID uuid;

        uuid = string2UUIDCache.getIfPresent(name.toLowerCase(Locale.ROOT));

        if (uuid == null && !string2UUIDStash.isEmpty()) {
            uuid = string2UUIDStash.get(name.toLowerCase(Locale.ROOT));
        }

        if (uuid != null) {
            return Bukkit.getOfflinePlayer(uuid);
        } else {
            Server server = Bukkit.getServer();
            result = findPlayerByName(name, server.getOnlinePlayers(), false);
            if (!useOfflineStash && result == null) {
                result = findPlayerByName(name, Arrays.asList(server.getOfflinePlayers()), true);
            }
            if (result == null) {
                result = Bukkit.getServer().getOfflinePlayer(name);
            }
            string2UUIDCache.put(name.toLowerCase(Locale.ROOT), result.getUniqueId());
        }
        return result;
    }

    public static OfflinePlayer findOfflinePlayerByUUID(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }
}
