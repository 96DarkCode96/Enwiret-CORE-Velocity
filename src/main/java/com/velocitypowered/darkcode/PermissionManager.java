package com.velocitypowered.darkcode;

import com.mongodb.client.model.Filters;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.console.VelocityConsole;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PermissionManager {
    public static final PermissionProvider PROVIDER = subject -> permission -> hasPermission(subject, permission);

    private static @NotNull Tristate hasPermission(@NotNull PermissionSubject subject, @NotNull String permission){
        if(!(subject instanceof ConnectedPlayer)){
            return (subject instanceof VelocityConsole ? ((VelocityConsole)subject).getPermissionValue(permission) : Tristate.UNDEFINED);
        }
        try{
            return Objects.requireNonNull(MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                    .getCollection("SUBJECT_" + ((ConnectedPlayer) subject).getUsername())
                    .find(Filters.eq("permission", permission))
                    .first()).get("value", Tristate.UNDEFINED);
        }catch(Throwable ignored){
            return Tristate.UNDEFINED;
        }
    }

    public static void setPermission(@NotNull PermissionSubject subject, @NotNull String permission, @NotNull Tristate tristate){
        if(!(subject instanceof ConnectedPlayer)){
            return;
        }
        Tristate res = hasPermission(subject, permission);
        if(tristate.equals(Tristate.UNDEFINED) && !res.equals(Tristate.UNDEFINED)){
            removePermission(subject, permission);
            return;
        }
        if(res == tristate){
            return;
        }
        if(res.equals(Tristate.UNDEFINED)){
            addPermission(subject, permission, tristate);
        }else{
            updatePermission(subject, permission, tristate);
        }
    }

    private static void updatePermission(@NotNull PermissionSubject subject, @NotNull String permission, @NotNull Tristate tristate) {
        if(MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                .getCollection("SUBJECT_" + ((ConnectedPlayer) subject).getUsername())
                        .countDocuments(Filters.eq("permission", permission)) > 1){
            removePermission(subject, permission);
            addPermission(subject, permission, tristate);
            return;
        }
        MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                .getCollection("SUBJECT_" + ((ConnectedPlayer) subject).getUsername())
                .updateOne(Filters.eq("permission", permission), new Document().append("permission", permission).append("value", tristate));
    }

    private static void removePermission(@NotNull PermissionSubject subject, @NotNull String permission) {
        MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                .getCollection("SUBJECT_" + ((ConnectedPlayer) subject).getUsername())
                .deleteMany(Filters.eq("permission", permission));
    }

    private static void addPermission(@NotNull PermissionSubject subject, @NotNull String permission, @NotNull Tristate tristate){
        if(tristate.equals(Tristate.UNDEFINED)){
            return;
        }
        MongoDB.getClient().getDatabase("Enwiret_PermissionManager").getCollection("SUBJECT_" + ((ConnectedPlayer) subject).getUsername())
                .insertOne(new Document().append("permission", permission).append("value", tristate));
    }
}