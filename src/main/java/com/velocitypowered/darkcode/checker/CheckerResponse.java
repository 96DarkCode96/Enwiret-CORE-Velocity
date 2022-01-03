package com.velocitypowered.darkcode.checker;

public enum CheckerResponse {
    ALLOW(true),DISALLOW(false);

    private final boolean b;

    CheckerResponse(boolean b) {
        this.b = b;
    }

    public boolean allowed() {
        return b;
    }
    public boolean disallowed() {
        return !b;
    }
}
