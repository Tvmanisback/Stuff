package dev.tvman.eagletalk;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EagleTalk management plugin for EaglerXServer's native Eaglercraft voice service.
 * Actual microphone/audio transport and client controls are provided by EaglerXServer
 * and a compatible Eaglercraft client, not by a second relay in this plugin.
 */
public final class EagleTalkPlugin extends JavaPlugin {
    private static final Pattern JSON_SETTING = Pattern.compile(
            "(?im)^([ \\t]*\"enable_voice_service\"[ \\t]*:[ \\t]*)(true|false)([ \\t]*(?:,[ \\t]*)?(?:#.*)?)$");
    private static final Pattern PLAIN_SETTING = Pattern.compile(
            "(?im)^([ \\t]*enable_voice_service[ \\t]*[:=][ \\t]*)(true|false)([ \\t]*(?:,[ \\t]*)?(?:#.*)?)$");

    @Override
    public void onEnable() {
        getLogger().info("EagleTalk loaded; voice transport is provided by EaglerXServer.");
        File settings = findSettingsFile();
        if (settings == null) {
            getLogger().warning("Could not find EaglerXServer settings file. Start EaglerXServer once, then use /voice status.");
            return;
        }
        Boolean enabled = readVoiceSetting(settings);
        if (Boolean.TRUE.equals(enabled)) {
            getLogger().info("EaglerXServer voice service is enabled in " + settings.getPath() + ".");
        } else {
            getLogger().warning("EaglerXServer voice service is disabled or could not be read. An operator can run /voice enable, then restart the server.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("voice")) return false;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.DARK_RED + "EagleTalk" + ChatColor.GRAY + " — Eaglercraft native voice chat");
            sender.sendMessage(ChatColor.YELLOW + "/voice status" + ChatColor.GRAY + " - check voice-service configuration");
            sender.sendMessage(ChatColor.YELLOW + "/voice enable" + ChatColor.GRAY + " - enable native voice (restart required)");
            sender.sendMessage(ChatColor.YELLOW + "/voice disable" + ChatColor.GRAY + " - disable native voice (restart required)");
            sender.sendMessage(ChatColor.GRAY + "Use a compatible EaglercraftX client with its native voice controls.");
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            boolean dependencyPresent = getServer().getPluginManager().getPlugin("EaglercraftXServer") != null;
            File settings = findSettingsFile();
            sender.sendMessage(ChatColor.DARK_RED + "EagleTalk status");
            sender.sendMessage(ChatColor.GRAY + "EaglerXServer dependency: " + (dependencyPresent ? ChatColor.GREEN + "detected" : ChatColor.RED + "missing"));
            if (settings == null) {
                sender.sendMessage(ChatColor.GRAY + "Settings file: " + ChatColor.RED + "not found");
                sender.sendMessage(ChatColor.YELLOW + "Start EaglerXServer once so it can create its settings file.");
            } else {
                Boolean enabled = readVoiceSetting(settings);
                sender.sendMessage(ChatColor.GRAY + "Settings file: " + ChatColor.GREEN + settings.getName());
                String value = enabled == null ? "unknown (unsupported format or key not found)" : (enabled ? "enabled" : "disabled");
                sender.sendMessage(ChatColor.GRAY + "Native voice service: " + (Boolean.TRUE.equals(enabled) ? ChatColor.GREEN : ChatColor.YELLOW) + value);
                sender.sendMessage(ChatColor.GRAY + "File path: " + settings.getPath());
            }
            sender.sendMessage(ChatColor.GRAY + "Voice audio requires a compatible Eaglercraft client and EaglerXServer.");
            return true;
        }

        if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) {
            if (!sender.hasPermission("eagletalk.admin")) {
                sender.sendMessage(ChatColor.RED + "You need eagletalk.admin (operators have it by default).");
                return true;
            }
            boolean enable = args[0].equalsIgnoreCase("enable");
            File settings = findSettingsFile();
            if (settings == null) {
                sender.sendMessage(ChatColor.RED + "EaglerXServer settings file was not found. Start EaglerXServer once, then try again.");
                return true;
            }
            try {
                if (!writeVoiceSetting(settings, enable)) {
                    sender.sendMessage(ChatColor.RED + "Couldn't find enable_voice_service in " + settings.getName() + ".");
                    sender.sendMessage(ChatColor.GRAY + "Edit EaglerXServer's voice_service.enable_voice_service setting manually.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "EaglerXServer native voice service set to " + enable + ".");
                sender.sendMessage(ChatColor.YELLOW + "Fully restart the server for EaglerXServer to apply the change.");
                if (enable) {
                    sender.sendMessage(ChatColor.GRAY + "Only enable voice on a server you trust; review EaglerXServer's security warning.");
                }
            } catch (IOException e) {
                getLogger().warning("Failed to update voice setting: " + e.getMessage());
                sender.sendMessage(ChatColor.RED + "Couldn't update the settings file. Check the server file permissions and console log.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("setup")) {
            sender.sendMessage(ChatColor.DARK_RED + "EagleTalk setup");
            sender.sendMessage(ChatColor.GRAY + "1. Install EaglerXServer and a compatible EaglercraftX client.");
            sender.sendMessage(ChatColor.GRAY + "2. Run /voice enable as an operator.");
            sender.sendMessage(ChatColor.GRAY + "3. Fully restart the server.");
            sender.sendMessage(ChatColor.GRAY + "4. Join with the compatible client and use its native voice controls.");
            sender.sendMessage(ChatColor.GRAY + "Voice audio is supplied by EaglerXServer/client; EagleTalk manages the server setting.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /voice [status|enable|disable|setup|help]");
        return true;
    }

    private File findSettingsFile() {
        File directory = new File("plugins/EaglercraftXServer");
        File[] files = directory.listFiles((dir, name) ->
                name.matches("settings\\.(cfg|gson|json|yaml|yml|toml)"));
        if (files == null || files.length == 0) return null;
        Arrays.sort(files, Comparator.comparingInt(file -> {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".gson") || name.endsWith(".json")) return 0;
            if (name.endsWith(".yaml") || name.endsWith(".yml")) return 1;
            if (name.endsWith(".toml")) return 2;
            return 3;
        }));
        return files[0];
    }

    private Boolean readVoiceSetting(File file) {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Matcher matcher = findSetting(content);
            if (matcher == null) return null;
            return Boolean.parseBoolean(matcher.group(2));
        } catch (IOException e) {
            getLogger().warning("Unable to read " + file.getPath() + ": " + e.getMessage());
            return null;
        }
    }

    private boolean writeVoiceSetting(File file, boolean enabled) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        Matcher matcher = findSetting(content);
        if (matcher == null) return false;
        String replacement = matcher.group(1) + enabled + matcher.group(3);
        String updated = content.substring(0, matcher.start()) + replacement + content.substring(matcher.end());
        Files.writeString(file.toPath(), updated, StandardCharsets.UTF_8);
        return true;
    }

    private Matcher findSetting(String content) {
        Matcher json = JSON_SETTING.matcher(content);
        if (json.find()) return json;
        Matcher plain = PLAIN_SETTING.matcher(content);
        return plain.find() ? plain : null;
    }
}
