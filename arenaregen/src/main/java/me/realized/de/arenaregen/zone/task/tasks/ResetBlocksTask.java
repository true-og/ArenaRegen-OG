package me.realized.de.arenaregen.zone.task.tasks;

import java.util.Queue;
import java.util.logging.Logger;
import me.realized.de.arenaregen.ArenaRegen;
import me.realized.de.arenaregen.nms.fallback.NMSHandler;
import me.realized.de.arenaregen.util.BlockInfo;
import me.realized.de.arenaregen.util.Callback;
import me.realized.de.arenaregen.util.Pair;
import me.realized.de.arenaregen.util.Position;
import me.realized.de.arenaregen.zone.Zone;
import me.realized.de.arenaregen.zone.task.Task;

public class ResetBlocksTask extends Task {

    private final Queue<Pair<Position, BlockInfo>> changed;

    public ResetBlocksTask(
            Logger logger,
            final ArenaRegen extension,
            final Zone zone,
            final Callback onDone,
            final Queue<Pair<Position, BlockInfo>> changed) {
        super(logger, extension, zone, onDone);
        this.changed = changed;
    }

    @Override
    public void run() {
        int count = 0;
        Pair<Position, BlockInfo> current;

        while ((current = changed.poll()) != null) {
            final Position pos = current.getKey();
            final BlockInfo info = current.getValue();
            handler.setBlockFast(zone.getWorld(), pos.getX(), pos.getY(), pos.getZ(), info.getData(), info.getType());
            count++;

            if (count >= config.getBlocksPerTick()) {
                return;
            }
        }

        cancel();

        // Skip relighting if using fallback handler
        if (handler instanceof NMSHandler) {
            zone.startSyncTaskTimer(null);
            zone.getArena().setDisabled(false);

            if (onDone != null) {
                onDone.call();
            }

            return;
        }

        this.logger.info("Starting to block relight task for zone " + this.zone.getName());
        zone.startSyncTaskTimer(new RelightBlocksTask(logger, extension, zone, onDone));
    }
}
