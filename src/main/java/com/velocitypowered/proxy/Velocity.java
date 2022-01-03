/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy;

import com.velocitypowered.darkcode.LogManager;
import com.velocitypowered.darkcode.Logger;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;

import java.text.DecimalFormat;

public class Velocity {

    private static final Logger logger;

    static {
        logger = LogManager.getLogger(Velocity.class);
        java.util.logging.LogManager.getLogManager().reset();
        java.util.logging.Logger loggera = java.util.logging.Logger.getLogger("org.slf4j");
        loggera.setLevel(java.util.logging.Level.OFF);
        System.setProperty("java.awt.headless", "true");
        if (System.getProperty("velocity.natives-tmpdir") != null) {
            System.setProperty("io.netty.native.workdir", System.getProperty("velocity.natives-tmpdir"));
        }
        if (System.getProperty("io.netty.leakDetection.level") == null) {
            ResourceLeakDetector.setLevel(Level.DISABLED);
        }
    }

    public static void main(String... args) {
        final ProxyOptions options = new ProxyOptions(args);
        if (options.isHelp()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        VelocityServer server = new VelocityServer(options);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.shutdown(false),
                "Shutdown thread"));
        double bootTime = (System.currentTimeMillis() - startTime) / 1000d;
        logger.info("Done ({0}s)!", new DecimalFormat("#.##").format(bootTime));
        server.getConsoleCommandSource().start();
        server.awaitProxyShutdown();
    }
}
