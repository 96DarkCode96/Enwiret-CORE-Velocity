package com.velocitypowered.darkcode;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.velocitypowered.proxy.VelocityServer;

public class MongoDB {

    private static MongoClient client;

    public static void init(VelocityServer server) {
        client = MongoClients.create(server.getConfiguration().getMongoDB());
    }

    public static MongoClient getClient() {
        if(client == null){
            return MongoClients.create();
        }
        return client;
    }
}
