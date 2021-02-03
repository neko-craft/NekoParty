package cn.apisium.nekoparty.games;

import cn.apisium.nekoparty.Knockout;
import cn.apisium.nekoparty.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

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

    abstract public void init();
    public void start() {
        started = true;
        Bukkit.getPluginManager().registerEvents(this, Main.INSTANCE);
    }
    public void stop() {
        if (!started) return;
        started = false;
        HandlerList.unregisterAll(this);
    }
    abstract public void clear();
    abstract public void sendIntroduction();
    abstract public void teleport();

    public void onKnockout(final Player player) { }
}
