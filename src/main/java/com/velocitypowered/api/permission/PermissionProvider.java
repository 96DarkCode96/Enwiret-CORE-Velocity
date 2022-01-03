/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.permission;

@FunctionalInterface
public interface PermissionProvider {

  PermissionFunction createFunction(PermissionSubject subject);
}
