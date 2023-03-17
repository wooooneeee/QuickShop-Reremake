/*
 * This file is a part of project QuickShop, the name is SimplePriceLimiter.java
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

package org.maxgamer.quickshop.shop;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.api.shop.PriceLimiter;
import org.maxgamer.quickshop.api.shop.PriceLimiterCheckResult;
import org.maxgamer.quickshop.api.shop.PriceLimiterStatus;
import org.maxgamer.quickshop.util.CalculateUtil;
import org.maxgamer.quickshop.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;

@Data
public class SimplePriceLimiter implements PriceLimiter {

    private double minPrice;
    private double maxPrice;
    private boolean allowFreeShop;
    private boolean wholeNumberOnly;
    private int maximumDigitsInPrice;
    @EqualsAndHashCode.Exclude
    private DecimalFormat decimalFormat;

    public SimplePriceLimiter(double minPrice, double maxPrice, boolean allowFreeShop, boolean wholeNumberOnly, int maximumDigitsInPrice) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.allowFreeShop = allowFreeShop;
        this.wholeNumberOnly = wholeNumberOnly;
        this.maximumDigitsInPrice = maximumDigitsInPrice;
    }

    public SimplePriceLimiter(QuickShop plugin) {
        this(plugin.getConfig().getDouble("shop.minimum-price"),
                plugin.getConfig().getInt("shop.maximum-price"),
                plugin.getConfig().getBoolean("shop.allow-free-shop"),
                plugin.getConfig().getBoolean("whole-number-prices-only"),
                plugin.getConfig().getInt("shop.maximum-digits-in-price", -1));
    }

    private DecimalFormat getDecimalFormat() {
        if (decimalFormat == null) {
            StringBuilder builder = new StringBuilder("#.");
            for (int i = 0; i <= (maximumDigitsInPrice + 1); i++) {
                builder.append("#");
            }
            decimalFormat = new DecimalFormat(builder.toString());
        }
        return decimalFormat;
    }

    @Override
    @NotNull
    public PriceLimiterCheckResult check(@NotNull ItemStack stack, double price) {
        SimplePriceLimiterCheckResult result = new SimplePriceLimiterCheckResult(PriceLimiterStatus.PASS, minPrice, maxPrice, price, maximumDigitsInPrice);

        if (Double.isInfinite(price) || Double.isNaN(price)) {
            return result.status(PriceLimiterStatus.NOT_VALID)
                    .priceShouldBe(minPrice);
        }
        if (maximumDigitsInPrice != -1) {
            String strFormat = getDecimalFormat().format(Math.abs(price)).replace(",", ".");
            String[] processedDouble = strFormat.split("\\.");
            if (processedDouble.length > 1) {
                if (processedDouble[1].length() > maximumDigitsInPrice) {
                    return result.status(PriceLimiterStatus.REACH_DIGITS_LIMIT)
                            .priceShouldBe(Double.parseDouble(processedDouble[0] + "." + processedDouble[1].substring(0, maximumDigitsInPrice)));
                }
            }
        }
        if (wholeNumberOnly) {
            try {
                //noinspection ResultOfMethodCallIgnored
                BigDecimal.valueOf(price).setScale(0, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException exception) {
                Util.debugLog(exception.getMessage());
                return result.status(PriceLimiterStatus.NOT_A_WHOLE_NUMBER)
                        .priceShouldBe(Math.floor(price));
            }
        }
        if (!allowFreeShop || price != 0) {
            if (price < minPrice) {
                return result.status(PriceLimiterStatus.REACHED_PRICE_MIN_LIMIT)
                        .priceShouldBe(minPrice);
            }
            if (maxPrice != -1 && price > maxPrice) {
                return result.status(PriceLimiterStatus.REACHED_PRICE_MAX_LIMIT)
                        .priceShouldBe(maxPrice);
            }
        }
        double perItemPrice;
        if (QuickShop.getInstance().isAllowStack()) {
            perItemPrice = CalculateUtil.divide(price, stack.getAmount());
        } else {
            perItemPrice = price;
        }
        Map.Entry<Double, Double> materialLimit = Util.getPriceRestriction(stack.getType());
        if (materialLimit != null) {
            //Updating the max and min price
            result.max(materialLimit.getKey()).min(materialLimit.getValue());
            if (!allowFreeShop || price != 0) {
                if (perItemPrice < materialLimit.getKey()) {
                    return result.status(PriceLimiterStatus.PRICE_RESTRICTED)
                            .priceShouldBe(materialLimit.getKey());
                } else if (perItemPrice > materialLimit.getValue()) {
                    return result.status(PriceLimiterStatus.PRICE_RESTRICTED)
                            .priceShouldBe(materialLimit.getValue());
                }
            }
        }
        return result;
    }
}
