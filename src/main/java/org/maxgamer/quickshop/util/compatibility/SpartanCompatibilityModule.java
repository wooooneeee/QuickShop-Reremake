/*
 * This file is a part of project QuickShop, the name is SpartanCompatibilityModule.java
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

package org.maxgamer.quickshop.util.compatibility;


import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.api.compatibility.AbstractQSCompatibilityModule;
import org.maxgamer.quickshop.util.Util;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public class SpartanCompatibilityModule extends AbstractQSCompatibilityModule implements Listener {

    public SpartanCompatibilityModule(QuickShop plugin) {
        super(plugin);
    }

    private final Set<UUID> ignoreList = new ConcurrentSkipListSet<>();

    @Override
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public @NotNull String getName() {
        return "Spartan";
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerViolation(PlayerViolationEvent event) {
        if (ignoreList.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void toggle(@NotNull Player player, boolean status) {
        if (status) {
            Util.debugLog(
                    "Calling Spartan continue follow " + player.getName() + " cheats detection.");
            ignoreList.remove(player.getUniqueId());
            for (Enums.HackType value : Enums.HackType.values()) {
                API.startCheck(player, value);
            }
        } else {
            ignoreList.add(player.getUniqueId());
            Util.debugLog(
                    "Calling Spartan ignore "
                            + player.getName()
                            + " cheats detection until we finished permission checks.");
            for (Enums.HackType value : Enums.HackType.values()) {
                API.stopCheck(player, value);
            }
        }
    }
}
