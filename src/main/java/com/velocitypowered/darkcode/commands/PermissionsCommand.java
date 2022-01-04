package com.velocitypowered.darkcode.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.darkcode.PermissionManager;
import com.velocitypowered.proxy.command.builtin.CommandMessages;
import net.kyori.adventure.identity.Identity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PermissionsCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        final CommandSource source = invocation.source();
        final String[] args = invocation.arguments();

        if (!source.getPermissionValue("*").equals(Tristate.TRUE)) {
            source.sendMessage(Identity.nil(), CommandMessages.PLAYERS_ONLY);
            return;
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return SimpleCommand.super.suggestAsync(invocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return SimpleCommand.super.hasPermission(invocation);
    }
}
