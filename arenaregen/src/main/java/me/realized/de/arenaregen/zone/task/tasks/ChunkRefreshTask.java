package me.realized.de.arenaregen.zone.task.tasks;

import me.realized.de.arenaregen.ArenaRegen;
import me.realized.de.arenaregen.util.Callback;
import me.realized.de.arenaregen.util.ChunkLoc;
import me.realized.de.arenaregen.zone.Zone;
import me.realized.de.arenaregen.zone.task.Task;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public class ChunkRefreshTask extends Task {

    private final Queue<ChunkLoc> chunks;

    public ChunkRefreshTask(Logger logger, final ArenaRegen extension, final Zone zone, final Callback onDone) {
        super(logger, extension, zone, onDone);
        this.chunks = new LinkedList<>(zone.getChunks());
    }

    @Override
    public void run() {
        ChunkLoc current = chunks.poll();

        if (current == null) {
            cancel();
            zone.startSyncTaskTimer(null);
            zone.getArena().setDisabled(false);

            if (onDone != null) {
                onDone.call();
            }

            return;
        }

        final Chunk chunk = zone.getMin().getWorld().getChunkAt(current.getX(), current.getZ());

        if (!chunk.isLoaded()) {
            return;
        }

        api.getServer().getOnlinePlayers().stream()
                .filter(player -> player.getWorld() == chunk.getWorld())
                .forEach(online -> {
                    Location chunkLocation = new Location(chunk.getWorld(), (chunk.getX() << 4) + 7.5d, 0, (chunk.getZ() << 4) + 7.5d);
                    Location playerLocation = online.getLocation().clone();
                    playerLocation.setY(0);

                    if (chunkLocation.distanceSquared(playerLocation) > (48 * 48)) {
                        return;
                    }
                    handler.sendChunkUpdate(online, chunk);
                });
    }

}
