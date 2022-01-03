package com.velocitypowered.darkcode;

import java.util.HashMap;
import java.util.Map;

public class LogManager {

    private static HashMap<Class, Logger> LOGGERS = new HashMap<>();
    private static HashMap<String, String> PACKAGE_NAME = new HashMap<>();

    public static Logger getLogger(Class clazz){
        if(LOGGERS.get(clazz) == null){
            String name = clazz.getName();
            for(Map.Entry<String, String> a : PACKAGE_NAME.entrySet()){
                if(clazz.getPackageName().startsWith(a.getKey())){
                    name = a.getValue();
                }
            }
            LOGGERS.put(clazz, new Logger(name, Logger.Level.ON));
            return getLogger(clazz);
        }
        return LOGGERS.get(clazz);
    }

    public static void setNameForPackage(String pack, String name){
        PACKAGE_NAME.put(pack, name);
    }

    static{
        setNameForPackage("com.velocitypowered", "VELOCITY");
        setNameForPackage("com.velocitypowered.darkcode", "CORE");
    }

}
