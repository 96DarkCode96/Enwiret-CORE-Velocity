package com.velocitypowered.darkcode;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LogManager {

    private static HashMap<Class, Logger> LOGGERS = new HashMap<>();
    private static HashMap<String, String> PACKAGE_NAME = new HashMap<>();

    public static Logger getLogger(Class clazz){
        if(LOGGERS.get(clazz) == null){
            String name = clazz.getName();
            for(Map.Entry<String, String> a : PACKAGE_NAME.entrySet()){
                if(clazz.getPackageName().startsWith(a.getKey()) || clazz.getName().startsWith(a.getKey())){
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

    private static PrintStream p = new PrintStream(new OutputStream() {
        StringBuilder a = new StringBuilder();
        @Override
        public void write(int b) {
            if(b == '\n'){
                try{
                    getLogger(Class.forName(getCallerClassName())).error(a.toString());
                }catch(Throwable ignored){}
                a = new StringBuilder();
                return;
            }
            a.append(new String(new byte[]{(byte)b}));
        }
        public String getCallerClassName() {
            StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
            return stElements[11].getClassName();
        }
    });

    static{
        setNameForPackage("com.velocitypowered", "VELOCITY");
        setNameForPackage("com.velocitypowered.darkcode", "CORE");
        getLogger(org.slf4j.helpers.Util.class).setLevel(Logger.Level.OFF);
        setNameForPackage("java.lang.Throwable", "THROWABLE");
        System.setErr(p);
    }

}
