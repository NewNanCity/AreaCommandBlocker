package io.github.gk0wk.areacommandblocker;

import co.aikar.commands.PaperCommandManager;
import io.github.gk0wk.areacommandblocker.area.Area;
import io.github.gk0wk.areacommandblocker.area.AreaContainer;
import io.github.gk0wk.areacommandblocker.area.VanillaAreaContainer;
import io.github.gk0wk.violet.config.ConfigManager;
import io.github.gk0wk.violet.config.ConfigUtil;
import io.github.gk0wk.violet.i18n.LanguageManager;
import io.github.gk0wk.violet.message.MessageManager;
import me.lucko.helper.Events;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AreaCommandBlocker extends ExtendedJavaPlugin {

    protected ConfigManager configManager;
    private LanguageManager languageManager;
    protected MessageManager messageManager;

    private static AreaCommandBlocker instance = null;
    public static AreaCommandBlocker getInstance() {
        return instance;
    }

    @Override
    protected void load() {
        // 初始化ConfigManager
        configManager = new ConfigManager(this);
        configManager.touch("config.yml");

        // 初始化LanguageManager
        try {
            Locale locale = new Locale("config");
            languageManager = new LanguageManager(this)
                    .register(locale, "config.yml")
                    .setMajorLanguage(locale);
        } catch (LanguageManager.FileNotFoundException | ConfigManager.UnknownConfigFileFormatException | IOException e) {
            e.printStackTrace();
            this.onDisable();
        }

        // 初始化MessageManager
        messageManager = new MessageManager(this)
                .setLanguageProvider(languageManager);
        messageManager.setPlayerPrefix(messageManager.sprintf("$msg.prefix$"));

        instance = this;
    }

    @Override
    protected void enable() {
        // 初始化CommandManager - 不能在load()里面初始化！
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.usePerIssuerLocale(true, false);
        try {
            commandManager.getLocales().loadYamlLanguageFile("config.yml", new Locale("config"));
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            this.onDisable();
        }

        // 注册指令
        commandManager.registerCommand(new AreaCommandBlockerCommands());

        // 注册事件
        Events.subscribe(PlayerCommandPreprocessEvent.class, EventPriority.HIGHEST)
                .filter(e -> !e.getPlayer().isOp())
                .filter(this::checkDeny)
                .handler(e -> {
                    messageManager.printf(e.getPlayer(), "$msg.command-deny$");
                    e.setCancelled(true);
                });

        // Containers
        areaContainers.add(new VanillaAreaContainer());

        // 载入配置
        reload();
    }

    private final Map<String, String[][]> commandGroups = new HashMap<>();

    public String[][] getGroup(String groupName) {
        return commandGroups.get(groupName);
    }

    private final Set<AreaContainer> areaContainers = new HashSet<>();

    protected void reload() {
        commandGroups.clear();
        areaContainers.forEach(AreaContainer::clear);
        try {
            // Read Command Group
            configManager.get("config.yml").getNode("command-group").getChildrenMap().forEach((key, value) -> {
                if (key instanceof String) {
                    Set<String[]> tmpGroup  = new HashSet<>();
                    ConfigUtil.setListIfNull(value).getList(Object::toString).forEach(command -> {
                        String[] tmpCommand = AreaCommandBlocker.normalizeCommand(command);
                        if (tmpCommand != null) {
                            tmpGroup.add(tmpCommand);
                        }
                    });
                    commandGroups.put((String) key, tmpGroup.toArray(new String[0][0]));
                }
            });

            // Read Areas
            configManager.get("config.yml").getNode("area").getChildrenMap().forEach((key, value) -> {
                if (key instanceof String) {
                    String area = (String) key;
                    String name = value.getNode("main").getString(null);
                    boolean whitelist = value.getNode("mode").getString("allow").equals("allow");
                    List<String> areaCommands = ConfigUtil.setListIfNull(value.getNode("list")).getList(Object::toString);

                    boolean accepted = false;
                    for (AreaContainer container : areaContainers) {
                        if (container.register(area, name, areaCommands, whitelist)) {
                            accepted = true;
                            break;
                        }
                    }

                    if (!accepted) {
                        messageManager.printf("$msg.load-failed$", area);
                    }
                }
            });
        } catch (IOException | ConfigManager.UnknownConfigFileFormatException e) {
            e.printStackTrace();
            this.onDisable();
        }

    }

    protected void listArea(CommandSender sender) {
        messageManager.printf(sender, "$msg.list-head$");
        areaContainers.forEach(containers ->
                containers.getAll().forEach(area -> {
                    messageManager.printf(sender, (area.whitelist ? "$msg.list-area-whitelist$" : "$msg.list-area-blacklist$"), area.name);
                    for (String[] pattern : area.contents) {
                        messageManager.printf(sender, "$msg.list-command$", pattern[0], pattern[1]);
                    }
                })
        );
    }

    private boolean checkDeny(PlayerCommandPreprocessEvent event) {
        for (AreaContainer container : areaContainers) {
            Area area = container.check(event.getPlayer().getLocation());
            if (area != null) {
                // 权限检查
                if (area.name != null && event.getPlayer().hasPermission("areacommandblocker.bypass." + area.name)) {
                    return false;
                }
                String[] commands = event.getMessage().split(" ")[0].split(":");
                String plugin = (commands.length > 1) ? commands[0] : "";
                String command = (commands.length > 1) ? commands[1] : commands[0];
                for (String[] patterns : area.contents) {
                    if (!patterns[0].isEmpty() && !plugin.matches(patterns[0])) {
                        continue;
                    }
                    if (command.matches(patterns[1])) {
                        return !area.whitelist;
                    }
                }
                return area.whitelist;
            }
        }
        return false;
    }

    private static final Pattern commandPattern = Pattern.compile("^/((?<plugin>[^:]*):)?(?<command>\\S+)$");
    public static String[] normalizeCommand(String command) {
        Matcher matcher = commandPattern.matcher(command);
        if (matcher.find()) {
            return new String[]{matcher.group("plugin"), matcher.group("command")};
        } else {
            return null;
        }
    }
}
