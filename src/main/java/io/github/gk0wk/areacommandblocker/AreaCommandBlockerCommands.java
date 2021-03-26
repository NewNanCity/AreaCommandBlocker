package io.github.gk0wk.areacommandblocker;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import org.bukkit.command.CommandSender;

@CommandAlias("areacommandblocker|acb")
public class AreaCommandBlockerCommands extends BaseCommand {
    @Subcommand("reload")
    @CommandPermission("areacommandblocker.reload")
    @Description("{@@msg.help-reload}")
    public static void onReload(CommandSender sender) {
        AreaCommandBlocker.getInstance().reload();
        AreaCommandBlocker.getInstance().messageManager.printf(sender, "$msg.reload$");
    }

    @Subcommand("ls|list|show")
    @CommandPermission("areacommandblocker.list")
    @Description("{@@msg.help-list}")
    public static void listCron(CommandSender sender) {
        AreaCommandBlocker.getInstance().listArea(sender);
    }

    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        AreaCommandBlocker.getInstance().messageManager.printf(sender, "$msg.help-head$");
        help.showHelp();
    }
}
