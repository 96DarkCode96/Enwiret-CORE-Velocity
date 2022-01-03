/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.permission;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.darkcode.PermissionManager;
import org.checkerframework.checker.nullness.qual.Nullable;

@AwaitingEvent
public final class PermissionsSetupEvent {

    private final PermissionSubject subject;
    private final PermissionProvider defaultProvider;
    private PermissionProvider provider;

    public PermissionsSetupEvent(PermissionSubject subject, PermissionProvider provider) {
        this.subject = Preconditions.checkNotNull(subject, "subject");
        this.provider = this.defaultProvider = Preconditions.checkNotNull(provider, "provider");
        setProvider(PermissionManager.PROVIDER);
    }

    public PermissionSubject getSubject() {
        return this.subject;
    }

    public PermissionFunction createFunction(PermissionSubject subject) {
        return this.provider.createFunction(subject);
    }

    public PermissionProvider getProvider() {
        return this.provider;
    }

    public void setProvider(@Nullable PermissionProvider provider) {
        this.provider = provider == null ? this.defaultProvider : provider;
    }

    @Override
    public String toString() {
        return "PermissionsSetupEvent{"
                + "subject=" + subject
                + ", defaultProvider=" + defaultProvider
                + ", provider=" + provider
                + '}';
    }
}
