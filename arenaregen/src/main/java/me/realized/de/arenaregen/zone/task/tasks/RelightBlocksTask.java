package me.realized.de.arenaregen.zone.task.tasks;

import java.util.logging.Logger;
import me.realized.de.arenaregen.ArenaRegen;
import me.realized.de.arenaregen.util.BlockUtil;
import me.realized.de.arenaregen.util.Callback;
import me.realized.de.arenaregen.zone.Zone;
import me.realized.de.arenaregen.zone.task.Task;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class RelightBlocksTask extends Task {

    private final Location min, max;

    private int x;

    public RelightBlocksTask(Logger logger, final ArenaRegen extension, final Zone zone, final Callback onDone) {
        super(logger, extension, zone, onDone);
        this.min = zone.getMin();
        this.max = zone.getMax();
        this.x = min.getBlockX();
    }

    @Override
    public void run() {
        for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
            for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                final Block block = zone.getWorld().getBlockAt(x, y, z);

                if (block.getType() == Material.AIR || BlockUtil.isSurrounded(block)) {
                    continue;
                }

                handler.updateLighting(zone.getWorld(), x, y, z);
            }
        }

        x++;

        if (x > max.getBlockX()) {
            cancel();
            this.logger.info("Starting to chunk refresh task for zone " + this.zone.getName());
            zone.startSyncTaskTimer(new ChunkRefreshTask(logger, extension, zone, onDone));
        }
    }
}
