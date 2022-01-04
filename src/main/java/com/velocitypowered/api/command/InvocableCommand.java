/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface InvocableCommand<I extends CommandInvocation<?>> extends Command {

    void execute(I invocation);

    default List<String> suggest(final I invocation) {
        return ImmutableList.of();
    }

    default CompletableFuture<List<String>> suggestAsync(final I invocation) {
        return CompletableFuture.completedFuture(suggest(invocation));
    }
    default boolean hasPermission(final I invocation) {
        return true;
    }
}
