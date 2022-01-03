package com.velocitypowered.darkcode;

import com.mongodb.client.model.Filters;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;

public class AuthSystem {

    public static boolean isPremium(String username) {
        return MongoDB.getClient().getDatabase("Enwiret_AuthSystem")
                .getCollection("SUBJECT_" + username)
                .find(Filters.eq("permission", permission);
    }
}
