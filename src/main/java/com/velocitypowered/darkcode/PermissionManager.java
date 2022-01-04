package com.velocitypowered.darkcode;

import com.mongodb.client.model.Filters;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.console.VelocityConsole;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public class PermissionManager {
    public static final PermissionProvider PROVIDER = subject -> permission -> {
        Tristate a = User.hasPermission(subject, permission);
        if(a.equals(Tristate.UNDEFINED)){
            a = Group.hasPermission(User.getGroup(subject), permission);
        }
        return a;
    };

    public static String[] getGroups(){
        try{
            ArrayList<String> array = new ArrayList<>();
            MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                    .listCollectionNames().into(array);
            return array.stream().filter(a -> a.startsWith("GROUP_")).sorted(Comparator.comparingInt(Group::getWeight)).toArray(String[]::new);
        }catch(Throwable ignored){
            return new String[]{};
        }
    }

    public static class User{
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

        public static String getGroup(@NotNull PermissionSubject subject){
            if(!(subject instanceof ConnectedPlayer)){
                return "default";
            }
            try{
                return Objects.requireNonNull(MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("SUBJECT_" + ((ConnectedPlayer) subject).getUsername())
                        .find(Filters.and(
                                Filters.eq("permission", "group"),
                                Filters.eq("value", Tristate.TRUE))
                        ).first()).get("data", "default");
            }catch(Throwable ignored){
                return "default";
            }
        }

        public static void setGroup(@NotNull PermissionSubject subject, @NotNull String group){
            if(!(subject instanceof ConnectedPlayer) || getGroup(subject).equals(group)){
                return;
            }
            try{
                MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("SUBJECT_" + ((ConnectedPlayer) subject).getUsername())
                        .deleteMany(Filters.eq("permission", "group"));
                MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("SUBJECT_" + ((ConnectedPlayer) subject).getUsername())
                        .insertOne(new Document().append("permission", "group").append("value", Tristate.TRUE).append("data", group));
            }catch(Throwable ignored){}
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
            MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                    .getCollection("SUBJECT_" + ((ConnectedPlayer) subject).getUsername())
                    .insertOne(new Document().append("permission", permission).append("value", tristate));
        }
    }

    public static class Group{

        public static int getWeight(@NotNull String subject){
            try{
                return Objects.requireNonNull(MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("GROUP_" + subject)
                        .find(Filters.and(
                                Filters.eq("permission", "weight"),
                                Filters.eq("value", Tristate.TRUE)
                        )).first()).get("data", -1);
            }catch(Throwable ignored){
                return -1;
            }
        }

        public static void setWeight(@NotNull String subject, int weight){
            try{
                MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("GROUP_" + subject)
                        .deleteMany(Filters.eq("permission", "weight"));
                MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("GROUP_" + subject)
                        .insertOne(new Document().append("permission", "weight").append("value", Tristate.TRUE).append("data", weight));
            }catch(Throwable ignored){}
        }

        public static String getPrefix(@NotNull String subject){
            try{
                return Objects.requireNonNull(MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("GROUP_" + subject)
                        .find(Filters.and(
                                Filters.eq("permission", "prefix"),
                                Filters.eq("value", Tristate.TRUE)
                        )).first()).get("data", "§7§lDefault");
            }catch(Throwable ignored){
                return "§7§lDefault";
            }
        }

        public static void setPrefix(@NotNull String subject, String prefix){
            try{
                MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("GROUP_" + subject)
                        .deleteMany(Filters.eq("permission", "prefix"));
                MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("GROUP_" + subject)
                        .insertOne(new Document().append("permission", "prefix").append("value", Tristate.TRUE).append("data", prefix));
            }catch(Throwable ignored){}
        }

        public static String getSuffix(@NotNull String subject){
            try{
                return Objects.requireNonNull(MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("GROUP_" + subject)
                        .find(Filters.and(
                                Filters.eq("permission", "suffix"),
                                Filters.eq("value", Tristate.TRUE)
                        )).first()).get("data", "");
            }catch(Throwable ignored){
                return "";
            }
        }

        public static void setSuffix(@NotNull String subject, String suffix){
            try{
                MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("GROUP_" + subject)
                        .deleteMany(Filters.eq("permission", "suffix"));
                MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("GROUP_" + subject)
                        .insertOne(new Document().append("permission", "suffix").append("value", Tristate.TRUE).append("data", suffix));
            }catch(Throwable ignored){}
        }

        public static @NotNull Tristate hasPermission(@NotNull String subject, @NotNull String permission){
            try{
                return Objects.requireNonNull(MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                        .getCollection("GROUP_" + subject)
                        .find(Filters.eq("permission", permission))
                        .first()).get("value", Tristate.UNDEFINED);
            }catch(Throwable ignored){
                return Tristate.UNDEFINED;
            }
        }

        public static void setPermission(@NotNull String subject, @NotNull String permission, @NotNull Tristate tristate){
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

        private static void updatePermission(@NotNull String subject, @NotNull String permission, @NotNull Tristate tristate) {
            if(MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                    .getCollection("GROUP_" + subject)
                    .countDocuments(Filters.eq("permission", permission)) > 1){
                removePermission(subject, permission);
                addPermission(subject, permission, tristate);
                return;
            }
            MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                    .getCollection("GROUP_" + subject)
                    .updateOne(Filters.eq("permission", permission), new Document().append("permission", permission).append("value", tristate));
        }

        private static void removePermission(@NotNull String subject, @NotNull String permission) {
            MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                    .getCollection("GROUP_" + subject)
                    .deleteMany(Filters.eq("permission", permission));
        }

        private static void addPermission(@NotNull String subject, @NotNull String permission, @NotNull Tristate tristate){
            if(tristate.equals(Tristate.UNDEFINED)){
                return;
            }
            MongoDB.getClient().getDatabase("Enwiret_PermissionManager")
                    .getCollection("GROUP_" + subject)
                    .insertOne(new Document().append("permission", permission).append("value", tristate));
        }
    }
}