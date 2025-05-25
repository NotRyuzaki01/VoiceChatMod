package me.not_ryuzaki.voiceChatMod;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoiceChatMod extends JavaPlugin implements CommandExecutor {

    private LuckPerms luckPerms;
    private Map<String, String> voicechatGroupMap;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadVoicechatGroups();
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        } else {
            getLogger().severe("LuckPerms not found. Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        PluginCommand vmuteCommand = getCommand("vmute");
        PluginCommand vunmuteCommand = getCommand("vunmute");

        if (vmuteCommand != null && vunmuteCommand != null) {
            vmuteCommand.setExecutor(this);
            vunmuteCommand.setExecutor(this);
        } else {
            getLogger().severe("Could not register commands. Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void loadVoicechatGroups() {
        voicechatGroupMap = new HashMap<>();

        ConfigurationSection section = getConfig().getConfigurationSection("voicechat-groups");
        if (section != null) {
            for (String group : section.getKeys(false)) {
                voicechatGroupMap.put(group.toLowerCase(), section.getString(group));
            }
        } else {
            getLogger().warning("No 'voicechat-groups' section found in config.yml.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("voicechatmod.use")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /" + cmd.getName() + " <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or offline!");
            return true;
        }

        User user = luckPerms.getUserManager().getUser(target.getUniqueId());
        if (user == null) {
            sender.sendMessage("§cCould not load player's permission data!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("vmute")) {
            handleVoiceMute(sender, target, user, true);
        } else if (cmd.getName().equalsIgnoreCase("vunmute")) {
            handleVoiceMute(sender, target, user, false);
        }

        return true;
    }

    private void handleVoiceMute(CommandSender sender, Player target, User user, boolean mute) {
        String currentMainGroup = user.getPrimaryGroup();

        if (mute) {
            if (isMutedGroup(currentMainGroup)) {
                sender.sendMessage("§c" + target.getName() + " is already voice muted!");
                return;
            }
            mutePlayer(sender, target, user, currentMainGroup);
        } else {
            if (!isMutedGroup(currentMainGroup)) {
                sender.sendMessage("§c" + target.getName() + " is not voice muted!");
                return;
            }
            unmutePlayer(sender, target, user, currentMainGroup);
        }
    }

    private boolean isMutedGroup(String group) {
        return voicechatGroupMap.containsValue(group.toLowerCase());
    }

    private void mutePlayer(CommandSender sender, Player target, User user, String currentGroup) {
        String mutedGroup = getMutedGroup(currentGroup);

        // Remove all current group inheritances
        List<Node> toRemove = new ArrayList<>();
        user.data().toCollection().forEach(node -> {
            if (node instanceof InheritanceNode) {
                toRemove.add(node);
            }
        });

        // Add the muted group
        luckPerms.getUserManager().modifyUser(target.getUniqueId(), u -> {
            toRemove.forEach(u.data()::remove);
            u.data().add(InheritanceNode.builder(mutedGroup).build());
            u.setPrimaryGroup(mutedGroup);
        });

        sendMuteMessage(sender, target, true, currentGroup, mutedGroup);
    }

    private String getOriginalGroup(String mutedGroup) {
        for (Map.Entry<String, String> entry : voicechatGroupMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(mutedGroup)) {
                return entry.getKey();
            }
        }
        return "default";
    }

    private void unmutePlayer(CommandSender sender, Player target, User user, String mutedGroup) {
        String originalGroup = getOriginalGroup(mutedGroup);

        // Remove all current group inheritances
        List<Node> toRemove = new ArrayList<>();
        user.data().toCollection().forEach(node -> {
            if (node instanceof InheritanceNode) {
                toRemove.add(node);
            }
        });

        // Add the original group back
        luckPerms.getUserManager().modifyUser(target.getUniqueId(), u -> {
            toRemove.forEach(u.data()::remove);
            u.data().add(InheritanceNode.builder(originalGroup).build());
            u.setPrimaryGroup(originalGroup);
        });

        sendMuteMessage(sender, target, false, mutedGroup, originalGroup);
    }

    private String getMutedGroup(String originalGroup) {
        return voicechatGroupMap.getOrDefault(originalGroup.toLowerCase(), voicechatGroupMap.get("default"));
    }


    private void sendMuteMessage(CommandSender sender, Player target, boolean muted, String fromGroup, String toGroup) {
        String action = muted ? "muted" : "unmuted";
        String color = muted ? "§c" : "§a";

        target.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(color + "You have been voice " + action + "!"));
        target.sendMessage(color + "You have been voice " + action + " by " + sender.getName());

        sender.sendMessage("§aSuccessfully voice " + action + " " + target.getName());
        sender.sendMessage("§7Group changed from " + fromGroup + " to " + toGroup);
    }
}