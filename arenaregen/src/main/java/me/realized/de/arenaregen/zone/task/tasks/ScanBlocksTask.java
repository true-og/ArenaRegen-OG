package me.realized.de.arenaregen.zone.task.tasks;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import me.realized.de.arenaregen.ArenaRegen;
import me.realized.de.arenaregen.util.BlockInfo;
import me.realized.de.arenaregen.util.Callback;
import me.realized.de.arenaregen.util.Pair;
import me.realized.de.arenaregen.util.Position;
import me.realized.de.arenaregen.zone.Zone;
import me.realized.de.arenaregen.zone.task.Task;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class ScanBlocksTask extends Task {

    private final Queue<Pair<Position, BlockInfo>> changed = new LinkedList<>();

    private final Location min, max;

    private int x;

    public ScanBlocksTask(Logger logger, final ArenaRegen extension, final Zone zone, final Callback onDone) {
        super(logger, extension, zone, onDone);
        this.min = zone.getMin();
        this.max = zone.getMax();
        this.x = min.getBlockX();
    }

    @Override
    public void run() {
        for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
            for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                final Block block = min.getWorld().getBlockAt(x, y, z);
                final Position position = new Position(block);
                final BlockInfo info = zone.getBlocks().get(position);

                if (info == null) {
                    // If no stored information is available (= air) but block is not air, set to air
                    if (block.getType() != Material.AIR) {
                        changed.add(new Pair<>(position, new BlockInfo()));
                    }

                    continue;
                } else if (info.matches(block)) {
                    continue;
                }

                changed.add(new Pair<>(position, info));
            }
        }

        x++;

        if (x > max.getBlockX()) {
            cancel();
            this.logger.info("Starting to block reset task for zone " + this.zone.getName());
            zone.startSyncTaskTimer(new ResetBlocksTask(logger, extension, zone, onDone, changed));
        }
    }
}
