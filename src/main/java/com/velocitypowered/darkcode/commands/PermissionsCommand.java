package com.velocitypowered.darkcode.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.darkcode.PermissionManager;
import com.velocitypowered.proxy.command.builtin.CommandMessages;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public class PermissionsCommand implements SimpleCommand {
    private final ProxyServer server;

    public PermissionsCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        final CommandSource source = invocation.source();
        final String[] args = invocation.arguments();

        if (!source.getPermissionValue("*").equals(Tristate.TRUE)) {
            source.sendMessage(Identity.nil(), CommandMessages.NO_PERMISSION.apply("*"));
            return;
        }

        if(e(args, a("groups"))){
            source.sendMessage(Identity.nil(), Component.empty());
            source.sendMessage(Identity.nil(), Component.text("Groups: ", NamedTextColor.GREEN));
            for(String group : PermissionManager.getGroups()){
                source.sendMessage(Identity.nil(), Component.text()
                        .content("  ")
                        .color(NamedTextColor.AQUA)
                        .content("[" + PermissionManager.Group.getWeight(group) + "]")
                        .color(NamedTextColor.DARK_GREEN)
                        .content(": ")
                        .content(group)
                        .hoverEvent((HoverEventSource<Component>) op ->
                                HoverEvent.showText(op.apply(Component.text()
                                        .append(Component.newline())
                                        .append(Component.text("§bPrefix§2: " + PermissionManager.Group.getPrefix(group)))
                                        .append(Component.text("§bSuffix§2: " + PermissionManager.Group.getSuffix(group)))
                                        .append(Component.newline())
                                .asComponent()
                        )))
                );
            }
            source.sendMessage(Identity.nil(), Component.empty());
        }else {
            source.sendMessage(Identity.nil(), Component.empty());
            source.sendMessage(Identity.nil(), Component.text("/pm groups", NamedTextColor.GREEN));
            source.sendMessage(Identity.nil(), Component.text("/pm user", NamedTextColor.GREEN));
            source.sendMessage(Identity.nil(), Component.text("/pm group", NamedTextColor.GREEN));
            source.sendMessage(Identity.nil(), Component.empty());
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
}
