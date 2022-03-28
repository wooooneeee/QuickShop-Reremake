/*
 * This file is a part of project QuickShop, the name is IErrorReporter.java
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
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public interface IErrorReporter {
    void unregister();

    void sendError(@NotNull Throwable throwable, @NotNull String... context);

    boolean canReport(@NotNull Throwable throwable);

    /**
     * Check a throw is cause by QS
     *
     * @param throwable Throws
     * @return Cause or not
     */
    default RollbarErrorReporter.PossiblyLevel checkWasCauseByQS(@Nullable Throwable throwable) {
        if (throwable == null) {
            return RollbarErrorReporter.PossiblyLevel.IMPOSSIBLE;
        }
        if (throwable.getMessage() == null) {
            return RollbarErrorReporter.PossiblyLevel.IMPOSSIBLE;
        }
        if (throwable.getMessage().contains("Could not pass event")) {
            if (throwable.getMessage().contains("QuickShop")) {
                return RollbarErrorReporter.PossiblyLevel.CONFIRM;
            } else {
                return RollbarErrorReporter.PossiblyLevel.IMPOSSIBLE;
            }
        }
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

        StackTraceElement[] stackTraceElements = throwable.getStackTrace();

        if (stackTraceElements.length == 0) {
            return RollbarErrorReporter.PossiblyLevel.IMPOSSIBLE;
        }

        if (stackTraceElements[0].getClassName().contains("org.maxgamer.quickshop") && stackTraceElements[1].getClassName().contains("org.maxgamer.quickshop")) {
            return RollbarErrorReporter.PossiblyLevel.CONFIRM;
        }

        long errorCount = Arrays.stream(stackTraceElements)
                .limit(3)
                .filter(stackTraceElement -> stackTraceElement.getClassName().contains("org.maxgamer.quickshop"))
                .count();

        if (errorCount > 0) {
            return RollbarErrorReporter.PossiblyLevel.MAYBE;
        } else if (throwable.getCause() != null) {
            return checkWasCauseByQS(throwable.getCause());
        }
        return RollbarErrorReporter.PossiblyLevel.IMPOSSIBLE;
    }

    void ignoreThrow();

    void ignoreThrows();

    void resetIgnores();

    boolean isEnabled();
}
