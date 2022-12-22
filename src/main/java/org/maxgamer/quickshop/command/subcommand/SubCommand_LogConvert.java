/*
 * This file is a part of project QuickShop, the name is SubCommand_LogConvert.java
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

package org.maxgamer.quickshop.command.subcommand;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.api.command.CommandHandler;
import org.maxgamer.quickshop.util.JsonUtil;
import org.maxgamer.quickshop.util.logging.container.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@AllArgsConstructor
public class SubCommand_LogConvert implements CommandHandler<CommandSender> {
    private final QuickShop plugin;

    private boolean convertLog(Path path) {
        try {
            List<String> outList = new ArrayList<>();
            List<String> stringList = Files.readAllLines(path.resolve("qs.log"));
            Gson gson = JsonUtil.standard();
            StringBuilder builder = new StringBuilder();
            for (String logline : stringList) {
                String[] splitedStr = logline.split("\\] ", 2);
                builder.setLength(0);
                builder.append(splitedStr[0]).append("] ");
                if (splitedStr.length == 2 && splitedStr[1].startsWith("{")) {
                    String[] keys = JsonUtil.readObject(splitedStr[1]).keySet().toArray(new String[0]);
                    if (keys.length >= 2) {
                        switch (keys[1].toLowerCase(Locale.ROOT)) {
                            case "from":
                                builder.append(gson.fromJson(splitedStr[1], EconomyTransactionLog.class).toReadableLog());
                                break;
                            case "player":
                                builder.append(gson.fromJson(splitedStr[1], PlayerEconomyPreCheckLog.class).toReadableLog());
                                break;
                            case "shop":
                                builder.append(gson.fromJson(splitedStr[1], ShopCreationLog.class).toReadableLog());
                                break;
                            case "moderator":
                                builder.append(gson.fromJson(splitedStr[1], ShopModeratorChangedLog.class).toReadableLog());
                                break;
                            case "oldprice":
                                builder.append(gson.fromJson(splitedStr[1], ShopPriceChangedLog.class).toReadableLog());
                                break;
                            case "type":
                                builder.append(gson.fromJson(splitedStr[1], ShopPurchaseLog.class).toReadableLog());
                                break;
                            case "reason":
                                builder.append(gson.fromJson(splitedStr[1], ShopRemoveLog.class).toReadableLog());
                                break;
                            default:
                                builder.append("Unknown log: ").append(splitedStr[1]);
                                break;
                        }

                    } else {
                        switch (keys[0].toLowerCase(Locale.ROOT)) {
                            case "rawdatabaseinfo":
                                builder.append(gson.fromJson(splitedStr[1], ShopStackingStatusChangeLog.class).toReadableLog());
                                break;
                            case "content":
                                builder.append(gson.fromJson(splitedStr[1], PluginGlobalAlertLog.class).toReadableLog());
                                break;
                            default:
                                builder.append("Unknown log: ").append(splitedStr[1]);
                                break;
                        }
                    }
                    outList.add(builder.toString());
                } else {
                    outList.add(logline);
                }
            }
            Files.write(path.resolve("readable_qs.log"), outList);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onCommand(CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        if (convertLog(plugin.getDataFolder().toPath())) {
            sender.sendMessage("Success! please check readable_qs.log in QuickShop Folder");
        } else {
            sender.sendMessage("Failed! please check console for more details");
        }
    }
}
