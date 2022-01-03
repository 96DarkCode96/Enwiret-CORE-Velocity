package com.velocitypowered.darkcode.checker;

public interface Checker<R> {

    Checker<String> NAME_CHECKER = object -> {
        if (object.contains(" ") || !object.matches("^[a-zA-Z0-9_]+$") || object.length() > 16 || object.length() < 3) {
            return CheckerResponse.DISALLOW;
        }
        return CheckerResponse.ALLOW;
    };

    CheckerResponse check(R object);
}
