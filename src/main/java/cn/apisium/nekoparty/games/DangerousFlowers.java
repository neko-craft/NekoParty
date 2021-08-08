package cn.apisium.nekoparty.games;

import cn.apisium.nekoparty.Knockout;
import cn.apisium.nekoparty.Main;
import cn.apisium.nekoparty.Utils;
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

@SuppressWarnings("deprecation")
public final class DangerousFlowers extends Game {
    private final static Material[] types = new Material[] {
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET,
            Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY,
            Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY, Material.FERN, Material.TALL_GRASS, Material.GRASS
    };
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
        setItem(Utils.SWORD_WITH_KNOCKBACK);
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
            block.setType(types[random.nextInt(types.length)]);
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
        knockout.title("§e危险的花朵!", "§b第二轮", true);
        Bukkit.broadcastMessage(getIntroduction("地上的花朵将会随机变成凋零玫瑰.", "你的目标是尽可能地活下去!"));
    }

    @Override
    public void teleport() {
        knockout.remains.forEach(it -> it.teleport(top[random.nextInt(top.length)].clone().add(0.5, 1, 0.5)));
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent e) {
        final Player player = e.getEntity();
        if (!knockout.remains.contains(player)) return;
        knockout.knockout(player);
        player.setHealth(20);
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent e) {
        final Location loc = e.getFrom();
        if (world != loc.getWorld() || loc.getX() < minX || loc.getX() > maxX || loc.getY() < minY ||
                loc.getY() > maxY || loc.getZ() < minZ || loc.getZ() > maxZ) {
            knockout.knockout(e.getPlayer());
        }
    }
}
