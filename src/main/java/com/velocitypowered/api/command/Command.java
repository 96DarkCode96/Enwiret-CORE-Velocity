/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import java.util.Arrays;

public interface Command {

    default String[] a(String... a){
        return a;
    }
    default boolean e(Object[] a, Object[] b){
        return Arrays.equals(a, b);
    }

}
