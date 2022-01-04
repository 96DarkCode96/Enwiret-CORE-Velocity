/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface SimpleCommand extends InvocableCommand<SimpleCommand.Invocation> {
    interface Invocation extends CommandInvocation<String @NonNull []> {
        String alias();
    }
}
