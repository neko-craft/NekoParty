package cn.apisium.nekoparty;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

public final class Utils {
    private static int I = 0;
    private final static CraftServer craftServer = (CraftServer) Bukkit.getServer();
    private final static DedicatedPlayerList dedicatedPlayerList = craftServer.getHandle();
    private static LightEngineThreaded lightEngineThreaded = null;

    public static EntityPlayer getNMSPlayer(final Player player) { return ((CraftPlayer) player).getHandle(); }
    private static WorldServer getNMSWorld(final World world) { return ((CraftWorld) world).getHandle(); }
    private static IChunkAccess getNMSChunk(final Chunk chunk) { return ((CraftChunk) chunk).getHandle(); }

    public static void init() {
        lightEngineThreaded = getNMSWorld(Bukkit.getWorld("world")).chunkProvider.getLightEngine();
    }

    public static void updateLight(final Chunk chunk) {
        lightEngineThreaded.a(getNMSChunk(chunk), true);
    }

    public static void playBreakingAnimation(final Block block, final int level) {
        dedicatedPlayerList.sendAll(new PacketPlayOutBlockBreakAnimation(I++,
                ((CraftBlock) block).getPosition(), level), getNMSWorld(block.getWorld()));
    }

    public static void later(long time, Runnable fn) {
        Main.INSTANCE.getServer().getScheduler().runTaskLater(Main.INSTANCE, fn, time);
    }

    public static BukkitTask timer(long time, IntConsumer fn) {
        final AtomicInteger i = new AtomicInteger();
        return Main.INSTANCE.getServer().getScheduler().runTaskTimer(Main.INSTANCE, () -> fn.accept(i.getAndIncrement()), time, time);
    }

    public static void timer(long time, int times, IntConsumer fn, Runnable cb) {
        final AtomicInteger i = new AtomicInteger();
        Main.INSTANCE.getServer().getScheduler().runTaskTimer(Main.INSTANCE, () -> {
            int it = i.getAndIncrement();
            if (it + 1 != times) fn.accept(it);
            else cb.run();
        }, time, time);
    }

    public static void timer(long time, long start, int times, IntConsumer fn, Runnable cb) {
        final AtomicInteger i = new AtomicInteger();
        final BukkitTask[] obj = new BukkitTask[1];
        obj[0] = Main.INSTANCE.getServer().getScheduler().runTaskTimer(Main.INSTANCE, () -> {
            int it = i.getAndIncrement();
            if (it + 1 <= times) fn.accept(it);
            else {
                cb.run();
                if (obj[0] != null) obj[0].cancel();
            }
        }, start, time);
    }
}
