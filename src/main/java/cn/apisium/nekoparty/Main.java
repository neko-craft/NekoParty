package cn.apisium.nekoparty;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.permission.Permissions;
import org.bukkit.plugin.java.annotation.plugin.*;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

@SuppressWarnings({"unused", "ConstantConditions"})
@Plugin(name = "NekoParty", version = "1.0")
@Description("A party in Minecraft.")
@Author("Shirasawa")
@Website("https://neko-craft.com")
@Permissions(@Permission(name = "nekoparty.use"))
@Commands(@org.bukkit.plugin.java.annotation.command.Command(name = "party", permission = "nekoparty.use"))
@ApiVersion(ApiVersion.Target.v1_13)
public final class Main extends JavaPlugin {
    public static Main INSTANCE;
    private Knockout knockout;

    { INSTANCE = this; }

    @Override
    public void onEnable() {
        Utils.init();
        getServer().getPluginCommand("party").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (knockout != null) {
                knockout.stop();
                knockout = null;
            }
            knockout = new Knockout(getServer().getWorld("world").getBlockAt(-166, 200, 259),
                    getServer().getOnlinePlayers().stream().filter(it -> !it.isOp()).collect(Collectors.toSet()));
            knockout.start();
        } else if (args[0].equals("clear")) knockout.clear();
        return true;
    }
}
