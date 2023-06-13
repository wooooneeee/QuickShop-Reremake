/*
 * This file is a part of project QuickShop, the name is SignListener.java
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

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.Cache;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.api.shop.Shop;
import org.maxgamer.quickshop.util.Util;

import java.util.Locale;

public class SignListener extends AbstractProtectionListener {
    public SignListener(@NotNull QuickShop plugin, @Nullable Cache cache) {
        super(plugin, cache);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChangeSign(SignChangeEvent e) {
        Block block = e.getBlock();
        if (Util.isWallSign(block.getType())) {
            String signSideLine0 = e.getLines()[0].toLowerCase(Locale.ROOT);
            String locketteHeader1 = getPlugin().getConfig().getString("lockette.private", "").toLowerCase(Locale.ROOT);
            String locketteHeader2 = getPlugin().getConfig().getString("lockette.more_users", "").toLowerCase(Locale.ROOT);
            if (signSideLine0.equals(locketteHeader1) || signSideLine0.equals(locketteHeader2)) {
                // Ignore changes on lockette sign
                return;
            }
            final Shop shop = getShopNextTo(block.getLocation());
            if (shop != null) {
                Util.debugLog("Player cannot change the shop infomation sign.");
                e.setCancelled(true);
            }
        }
    }

    /**
     * Gets the shop a sign is attached to
     *
     * @param loc The location of the sign
     * @return The shop
     */
    @Nullable
    private Shop getShopNextTo(@NotNull Location loc) {
        final Block b = Util.getAttached(loc.getBlock());
        // Util.getAttached(b)
        if (b == null) {
            return null;
        }
        return getShopPlayer(b.getLocation(), false);
    }
}
