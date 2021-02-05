package cn.apisium.nekoparty.games;

import cn.apisium.nekoparty.Knockout;
import cn.apisium.nekoparty.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

public final class DangerousFlowers extends Game {
    private final static Random random = new Random();
    private final LinkedList<Block> flowers = new LinkedList<>();
    private final Location[] top = new Location[32 * 32];
    private BukkitTask timer;
    private final int maxX, minX, maxY, minY, maxZ, minZ;
    public DangerousFlowers(Block block, Knockout knockout) {
        super(block, knockout);
        minX = block.getX() - 2;
        minZ = block.getZ() - 2;
        maxX = minX + 36;
        maxZ = minZ + 36;
        maxY = block.getY() + 10;
        minY = block.getY() - 15;
    }

    @Override
    public void start() {
        super.start();
        timer = Bukkit.getScheduler().runTaskTimer(Main.INSTANCE, () -> {
            while (random.nextInt(7) != 0 && !flowers.isEmpty()) {
                final Block block = flowers.pop();
                block.setType(Material.WITHER_ROSE);
                block.getRelative(0, -1, 0).setType(Material.PODZOL);
            }
            if (flowers.isEmpty()) {
                timer.cancel();
                timer = null;
            }
        }, 100, 20);
    }

    @Override
    public void stop() {
        super.stop();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void init() {
        flowers.clear();
        final Location loc = center.getLocation();
        for (int i = 0, t = 0; i < 32; i++) for (int j = 0; j < 32; j++) {
            int y = random.nextBoolean() ? random.nextInt(3) : 0;
            for (int k = 0; k <= y; k++) loc.clone().add(i, k, j).getBlock().setType(k == y ? Material.GRASS_BLOCK : Material.DIRT);
            final Block block = (top[t++] = loc.clone().add(i, y, j)).clone().add(0, 1, 0).getBlock();
            block.setType(randomCropType());
            flowers.add(block);
        }
        Collections.shuffle(flowers, random);
    }

    @Override
    public void clear() {
        flowers.clear();
        final Location loc = center.getLocation();
        for (int i = 0; i < 32; i++) for (int j = 0; j < 32; j++) for (int k = 4; k >= 0; k--){
            loc.clone().add(i, k, j).getBlock().setType(Material.AIR);
        }
        loc.getWorld().getEntities().forEach(it -> {
            if (it.getType() == EntityType.DROPPED_ITEM) it.remove();
        });
    }

    @Override
    public void sendIntroduction() {
        knockout.title("§e跳跃吧!", "§b第二轮");
        Bukkit.broadcastMessage("§b§m               §r §a[§e游戏介绍§a] §b§m               \n§a  地上的花朵将会随机变成凋零玫瑰.\n§b  你的目标是尽可能地活下去!\n§b§m                                                          §r\n");
    }

    @Override
    public void teleport() {
        knockout.remains.forEach(it -> it.teleport(top[random.nextInt(top.length)]));
    }

    @EventHandler
    public void on(final PlayerDeathEvent e) {
        final Player player = e.getEntity();
        if (!knockout.remains.contains(player)) return;
        knockout.knockout(player);
        player.setHealth(20);
        e.setCancelled(true);
    }

    private static Material randomCropType() {
        switch (random.nextInt(16)) {
            case 1: return Material.DANDELION;
            case 2: return Material.POPPY;
            case 3: return Material.BLUE_ORCHID;
            case 4: return Material.ALLIUM;
            case 5: return Material.AZURE_BLUET;
            case 6: return Material.RED_TULIP;
            case 7: return Material.ORANGE_TULIP;
            case 8: return Material.WHITE_TULIP;
            case 9: return Material.PINK_TULIP;
            case 10: return Material.OXEYE_DAISY;
            case 11: return Material.CORNFLOWER;
            case 12: return Material.LILY_OF_THE_VALLEY;
            case 13: return Material.FERN;
            case 14: return Material.TALL_GRASS;
            default: return Material.GRASS;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent e) {
        final Location loc = e.getFrom();
        if (world != loc.getWorld() || loc.getX() < minX || loc.getX() > maxX || loc.getY() < minY ||
                loc.getY() > maxY || loc.getZ() < minZ || loc.getZ() > maxZ) {
            if (knockout.remains.contains(e.getPlayer())) knockout.knockout(e.getPlayer());
        }
    }
}
