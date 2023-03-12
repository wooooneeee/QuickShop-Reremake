/*
 * This file is a part of project QuickShop, the name is PriceLimiterCheckResult.java
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

package org.maxgamer.quickshop.api.shop;

import org.bukkit.command.CommandSender;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.util.MsgUtil;

/**
 * Result of PriceLimiter check
 */
public interface PriceLimiterCheckResult {
    /**
     * Getting final result
     *
     * @return Result
     */
    PriceLimiterStatus getStatus();

    /**
     * Getting this type of item min allowed price is
     *
     * @return Min price
     */
    double getMin();

    /**
     * Getting this type of item max allowed price is
     *
     * @return Max price
     */
    double getMax();

    /**
     * Get the price max digit should be
     *
     * @return the max digit limit
     */
    default int getMaxDigit() {
        return -1;
    }

    /**
     * Get the price should be in this limitation
     *
     * @return the price after adjusted
     */
    default double getPriceShouldBe() {
        return getMin();
    }

    default void sendErrorMsg(QuickShop plugin, CommandSender sender, String input, String itemName) {
        boolean decFormat = plugin.getConfig().getBoolean("use-decimal-format");
        String maxPriceStr;
        String minPriceStr;
        if (decFormat) {
            maxPriceStr = MsgUtil.decimalFormat(getMax());
            minPriceStr = MsgUtil.decimalFormat(getMin());
        } else {
            maxPriceStr = Double.toString(getMax());
            minPriceStr = Double.toString(getMin());
        }
        switch (getStatus()) {
            case REACHED_PRICE_MIN_LIMIT:
                plugin.text().of(sender, "price-too-cheap",
                        minPriceStr).send();
                break;
            case REACHED_PRICE_MAX_LIMIT:
                plugin.text().of(sender, "price-too-high",
                        maxPriceStr).send();
                break;
            case NOT_VALID:
                plugin.text().of(sender, "not-a-number", input).send();
                break;
            case NOT_A_WHOLE_NUMBER:
                plugin.text().of(sender, "not-a-integer", input).send();
                break;
            case REACH_DIGITS_LIMIT:
                plugin.text().of(sender, "digits-reach-the-limit", String.valueOf(getMaxDigit())).send();
                break;
            default:
                plugin.text().of(sender, "restricted-prices",
                        itemName,
                        minPriceStr,
                        maxPriceStr).send();
        }
    }
}
