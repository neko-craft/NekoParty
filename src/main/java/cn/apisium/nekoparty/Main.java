package cn.apisium.nekoparty;

import cn.apisium.nekoparty.games.*;
import org.bukkit.SoundCategory;
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
            knockout = new Knockout(getServer().getWorld("world").getBlockAt(2261, 220, 716),
                    getServer().getOnlinePlayers().stream().filter(it -> !it.isOp()).collect(Collectors.toSet()),
                    LetsJump.class, DangerousFlowers.class, AnvilRain.class, RememberTheBlock.class, LastOfUS.class);
            knockout.start();
        } else switch (args[0]) {
            case "clear":
                if (knockout != null) knockout.clear();
                break;
            case "play":
                getServer().getOnlinePlayers().forEach(it -> it.playSound(it.getLocation(), "com.neko-craft.game", SoundCategory.RECORDS, 1f, 1f));
                break;
            case "stop":
                getServer().getOnlinePlayers().forEach(it -> it.stopSound("com.neko-craft.game", SoundCategory.RECORDS));
                break;
        }
        return true;
    }
}
