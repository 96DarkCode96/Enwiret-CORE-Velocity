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

package com.velocitypowered.proxy.console;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.darkcode.LogManager;
import com.velocitypowered.darkcode.Logger;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.util.ClosestLocaleMatcher;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import java.util.List;
import java.util.Locale;

import static com.velocitypowered.api.permission.PermissionFunction.ALWAYS_TRUE;

public final class VelocityConsole extends SimpleTerminalConsole implements ConsoleCommandSource {

    private static final Logger logger = LogManager.getLogger(VelocityConsole.class);

    private final VelocityServer server;
    private final PermissionFunction permissionFunction = ALWAYS_TRUE;

    public VelocityConsole(VelocityServer server) {
        this.server = server;
    }

    @Override
    public void sendMessage(@NonNull Identity identity, @NonNull Component message) {
        Component translated = GlobalTranslator.render(message, ClosestLocaleMatcher.INSTANCE
                .lookupClosest(Locale.getDefault()));
        logger.info(LegacyComponentSerializer.legacySection().serialize(translated));
    }

    @Override
    public @NonNull Tristate getPermissionValue(@NonNull String permission) {
        return this.permissionFunction.getPermissionValue(permission);
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        return super.buildReader(builder
                .appName("Velocity")
                .completer((reader, parsedLine, list) -> {
                    try {
                        List<String> offers = this.server.getCommandManager()
                                .offerSuggestions(this, parsedLine.line())
                                .join();
                        for (String offer : offers) {
                            list.add(new Candidate(offer));
                        }
                    } catch (Exception e) {
                        logger.error("An error occurred while trying to perform tab completion.", e);
                    }
                })
        );
    }

    @Override
    protected boolean isRunning() {
        return !this.server.isShutdown();
    }

    @Override
    protected void runCommand(String command) {
        try {
            if (!this.server.getCommandManager().executeAsync(this, command).join()) {
                sendMessage(Component.translatable("velocity.command.command-does-not-exist",
                        NamedTextColor.RED));
            }
        } catch (Exception e) {
            logger.error("An error occurred while running this command.", e);
        }
    }

    @Override
    protected void shutdown() {
        this.server.shutdown(true);
    }

}
