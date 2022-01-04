package com.velocitypowered.darkcode;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AuthSystem {

    public static boolean isPremium(String username) {
        return getUserNameStatus(username).equals(Status.PREMIUM);
    }

    public static Status getUserNameStatus(String username){
        try{
            return Objects.requireNonNull(MongoDB.getClient()
                            .getDatabase("Enwiret_AuthSystem")
                            .getCollection("SUBJECT_" + username).find().first())
                    .get("status", Status.UNKNOWN);
        }catch(Throwable ignored){
            return Status.UNKNOWN;
        }
    }

    public static boolean comparePassword(String username, @NotNull String password){
        try{
            return Objects.requireNonNull(MongoDB.getClient()
                    .getDatabase("Enwiret_AuthSystem")
                    .getCollection("SUBJECT_" + username)
                    .find().first()).get("password", "").equals(password);
        }catch(Throwable ignored){
            return false;
        }
    }

    public enum Status{
        PREMIUM, WAREZ, UNKNOWN
    }

    //  Database: Enwiret_AuthSystem
    //    Collection: SUBJECT_96DarkCode96
    //      Document:
    //        "status": AuthSystem.Status.PREMIUM
    //        "password": "passwd"
    //
}
