package cn.apisium.nekoparty.games;

import cn.apisium.nekoparty.Knockout;
import cn.apisium.nekoparty.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public abstract class Game implements Listener {
    final Block center;
    final World world;
    final Knockout knockout;
    boolean started = false;

    public Game(final Block block, final Knockout knockout) {
        center = block;
        this.knockout = knockout;
        world = block.getWorld();
    }

    public void start() {
        started = true;
        Bukkit.getPluginManager().registerEvents(this, Main.INSTANCE);
    }
    public void stop() {
        if (!started) return;
        started = false;
        HandlerList.unregisterAll(this);
    }
    public int getRemainCount() { return -1; }
    public void setItem(ItemStack is) {
        knockout.remains.forEach(it -> {
            PlayerInventory inv = it.getInventory();
            inv.clear();
            inv.setItemInMainHand(is);
            it.updateInventory();
        });
    }
    abstract public void init();
    abstract public void clear();
    abstract public void sendIntroduction();
    abstract public void teleport();

    public static String getIntroduction(String line1, String line2) {
        return "§b§m               §r §a[§e游戏介绍§a] §b§m               \n  §a" + line1 + "\n  §b" + line2 +
                "\n§b§m                                                          §r\n";
    }

    public void onKnockout(final Player player) { }
}
