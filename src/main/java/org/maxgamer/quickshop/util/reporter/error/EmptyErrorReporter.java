/*
 * This file is a part of project QuickShop, the name is EmptyErrorReporter.java
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

package org.maxgamer.quickshop.util.reporter.error;

import org.jetbrains.annotations.NotNull;

public class EmptyErrorReporter implements IErrorReporter {
    @Override
    public void unregister() {

    }

    @Override
    public void sendError(@NotNull Throwable throwable, @NotNull String... context) {

    }

    @Override
    public boolean canReport(@NotNull Throwable throwable) {
        return false;
    }

    @Override
    public void ignoreThrow() {

    }

    @Override
    public void ignoreThrows() {

    }

    @Override
    public void resetIgnores() {

    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
