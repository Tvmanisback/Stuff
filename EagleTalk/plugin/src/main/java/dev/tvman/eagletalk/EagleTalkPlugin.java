package dev.tvman.eagletalk;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper/Spigot/Bukkit helper for EaglerXServer's built-in voice service.
 * Audio transport and client voice controls remain in EaglerXServer/client;
 * this plugin does not invent a separate relay or claim to control client audio.
 */
public final class EagleTalkPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("EagleTalk loaded. It uses EaglerXServer's native voice service.");
        getLogger().info("Check plugins/EaglercraftXServer/settings.cfg and enable voice_service.enable_voice_service.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("voice")) return false;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.DARK_RED + "EagleTalk" + ChatColor.GRAY + " — EaglerXServer native voice");
            sender.sendMessage(ChatColor.YELLOW + "/voice status" + ChatColor.GRAY + " - check plugin/config status");
            sender.sendMessage(ChatColor.YELLOW + "/voice setup" + ChatColor.GRAY + " - show setup steps");
            sender.sendMessage(ChatColor.GRAY + "Use the Eaglercraft client's native voice controls to mute yourself or other players.");
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            boolean eaglerPresent = getServer().getPluginManager().getPlugin("EaglercraftXServer") != null;
            sender.sendMessage(ChatColor.DARK_RED + "EagleTalk status");
            sender.sendMessage(ChatColor.GRAY + "EaglerXServer dependency: " + (eaglerPresent ? ChatColor.GREEN + "detected" : ChatColor.RED + "missing"));
            File settings = new File("plugins/EaglercraftXServer/settings.cfg");
            sender.sendMessage(ChatColor.GRAY + "EaglerXServer settings.cfg: " + (settings.isFile() ? ChatColor.GREEN + "found" : ChatColor.RED + "not found"));
            sender.sendMessage(ChatColor.GRAY + "Voice setting: verify voice_service.enable_voice_service = true in settings.cfg.");
            sender.sendMessage(ChatColor.GRAY + "If you changed settings, restart the server and test with a compatible Eaglercraft client.");
            return true;
        }

        if (args[0].equalsIgnoreCase("setup")) {
            if (!sender.hasPermission("eagletalk.admin")) {
                sender.sendMessage(ChatColor.RED + "You need eagletalk.admin (operators have it by default).");
                return true;
            }
            sender.sendMessage(ChatColor.DARK_RED + "EagleTalk setup");
            sender.sendMessage(ChatColor.GRAY + "1. Install EaglerXServer in the plugins folder.");
            sender.sendMessage(ChatColor.GRAY + "2. Start once, then open plugins/EaglercraftXServer/settings.cfg.");
            sender.sendMessage(ChatColor.GRAY + "3. Set voice_service.enable_voice_service to true.");
            sender.sendMessage(ChatColor.GRAY + "4. Keep voice enabled only on worlds you intend to support.");
            sender.sendMessage(ChatColor.GRAY + "5. Restart and connect using a compatible Eaglercraft client.");
            sender.sendMessage(ChatColor.GRAY + "Mute/self-mute controls come from the client's native voice UI.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /voice [status|setup|help]");
        return true;
    }
}
