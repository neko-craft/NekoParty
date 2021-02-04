package cn.apisium.nekoparty.games;

import cn.apisium.nekoparty.Knockout;
import cn.apisium.nekoparty.Main;
import cn.apisium.nekoparty.Utils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Random;

public final class LastOfUS extends Game {
    private final static Random random = new Random();
    private final int minX, minZ, maxX, maxZ, maxY, minY;
    private HashSet<Block> blocks1 = null, blocks2 = null, blocks3 = null;
    private BukkitTask timer = null;

    public LastOfUS(final Block block, final Knockout knockout) {
        super(block, knockout);
        minX = block.getX();
        minZ = block.getZ();
        maxX = minX + 36;
        maxZ = minZ + 36;
        maxY = block.getY() + 20;
        minY = block.getY() - 130;
    }

    @Override
    public void init() {
        final Location loc = center.getLocation();
        for (int k = 0; k < 12; k++) for (int i = 0; i < 36; i++) for (int j = 0; j < 36; j++) {
            loc.clone().add(i, -k * 10, j).getBlock().setType(Material.PACKED_ICE, false);
        }
    }

    @Override
    public void clear() {
        final Location loc = center.getLocation();
        for (int k = 0; k < 12; k++) for (int i = 0; i < 36; i++) for (int j = 0; j < 36; j++) {
            loc.clone().add(i, -k * 10, j).getBlock().setType(Material.AIR, false);
        }
    }

    @Override
    public void sendIntroduction() {
        knockout.title("§e最后生还者", "§b最后一轮");
        Bukkit.broadcastMessage("§b§m               §r §a[§e游戏介绍§a] §b§m               \n§a  玩家将站立在冰平台上, 当游戏开始后, 被站立过的冰块将会破碎.\n§b  你的目标是存活到最后, 如果不小心失足掉下去将会被淘汰!\n§b§m                                                          §r\n");
    }

    @Override
    public void teleport() {
        knockout.remains.forEach(it -> it.teleport(center.getLocation().clone().add(2 + random.nextInt(34), 1, 2 + random.nextInt(34))));
    }

    @Override
    public void start() {
        if (started) return;
        blocks1 = new HashSet<>();
        blocks2 = new HashSet<>();
        blocks3 = new HashSet<>();
        timer = Bukkit.getScheduler().runTaskTimer(Main.INSTANCE, () -> {
            HashSet<Chunk> chunks = new HashSet<>();
            blocks3.forEach(block -> {
                Chunk chunk = block.getChunk();
                if (!chunks.add(chunk)) Utils.updateLight(chunk);
                block.setType(Material.AIR, false);
            });
            blocks3 = blocks2;
            blocks2 = blocks1;
            blocks1 = new HashSet<>();
            blocks2.forEach(it -> Utils.playBreakingAnimation(it, 2));
            blocks3.forEach(it -> Utils.playBreakingAnimation(it, 5));
        }, 15, 15);
        super.start();
    }

    @Override
    public void stop() {
        if (!started) return;
        super.stop();
        timer.cancel();
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent e) {
        final Location loc = e.getFrom().add(0, -1, 0);
        if (world != loc.getWorld() || loc.getX() < minX || loc.getX() > maxX || loc.getY() < minY ||
                loc.getY() > maxY || loc.getZ() < minZ || loc.getZ() > maxZ) {
            if (knockout.remains.contains(e.getPlayer())) knockout.knockout(e.getPlayer());
            return;
        }
        addBlock(loc);
        addBlock(loc.clone().add(0, 0, 1));
        addBlock(loc.clone().add(0, 0, -1));
        addBlock(loc.clone().add(1, 0, 0));
        addBlock(loc.clone().add(1, 0, 1));
        addBlock(loc.clone().add(1, 0, -1));
        addBlock(loc.clone().add(-1, 0, 0));
        addBlock(loc.clone().add(-1, 0, 1));
        addBlock(loc.add(-1, 0, -1));
    }

    private void addBlock(final Location loc) {
        final Block block = loc.getBlock();
        if (block.getType() != Material.PACKED_ICE) return;
        blocks1.add(block);
    }
}
