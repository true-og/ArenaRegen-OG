package me.realized.de.arenaregen.zone;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import lombok.Getter;
import me.realized.de.arenaregen.ArenaRegen;
import me.realized.de.arenaregen.config.Config;
import me.realized.de.arenaregen.nms.NMS;
import me.realized.de.arenaregen.util.*;
import me.realized.de.arenaregen.zone.task.Task;
import me.realized.de.arenaregen.zone.task.tasks.FilterBlocksTask;
import me.realized.de.arenaregen.zone.task.tasks.ScanBlocksTask;
import me.realized.duels.api.Duels;
import me.realized.duels.api.arena.Arena;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Zone {

    @Getter
    private final Duels api;

    private final ArenaRegen extension;
    private final NMS handler;

    @Getter
    private final Config config;

    @Getter
    private final Arena arena;

    @Getter
    private Location min, max;

    private final Logger logger;

    @Getter
    private File file;

    @Getter
    private Task task;

    @Getter
    private volatile Map<Position, BlockInfo> blocks = new HashMap<>();

    @Getter
    private final Set<ChunkLoc> chunks = new HashSet<>();

    //    @Getter
    //    private final List<Entity> spawnedEntities = new ArrayList<>();

    private Set<Position> changedBlocks = new HashSet<>();

    Zone(
            final ArenaRegen extension,
            final Duels api,
            final Arena arena,
            final File folder,
            final Location first,
            final Location second) {
        this.api = api;
        this.extension = extension;
        this.handler = extension.getHandler();
        this.config = extension.getConfiguration();
        this.logger = extension.getApi().getLogger();
        this.arena = arena;
        this.file = new File(folder, arena.getName() + ".txt");
        this.min = new Location(
                first.getWorld(),
                Math.min(first.getBlockX(), second.getBlockX()),
                Math.min(first.getBlockY(), second.getBlockY()),
                Math.min(first.getBlockZ(), second.getBlockZ()));
        this.max = new Location(
                first.getWorld(),
                Math.max(first.getBlockX(), second.getBlockX()),
                Math.max(first.getBlockY(), second.getBlockY()),
                Math.max(first.getBlockZ(), second.getBlockZ()));

        final Map<Position, BlockInfo> blocks = new HashMap<>();

        BlockUtil.runForCuboid(min, max, block -> {
            // Only store non-air blocks
            if (block.getType() == Material.AIR) {
                return;
            }

            blocks.put(new Position(block), new BlockInfo(block.getState()));
        });

        if (!config.isTrackBlockChanges()) {
            this.blocks = blocks;
        }

        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(min.getWorld().getName());
            writer.newLine();
            writer.write(String.valueOf(min.getBlockX()));
            writer.newLine();
            writer.write(String.valueOf(min.getBlockY()));
            writer.newLine();
            writer.write(String.valueOf(min.getBlockZ()));
            writer.newLine();
            writer.write(String.valueOf(max.getBlockX()));
            writer.newLine();
            writer.write(String.valueOf(max.getBlockY()));
            writer.newLine();
            writer.write(String.valueOf(max.getBlockZ()));
            writer.newLine();

            for (Map.Entry<Position, BlockInfo> entry : blocks.entrySet()) {
                writer.write(entry.getKey().toString() + ":" + entry.getValue().toString());
                writer.newLine();
            }
        } catch (IOException ex) {
            extension.error("Could not save reset zone '" + getName() + "'!", ex);
        }

        loadChunks();
    }

    Zone(final ArenaRegen extension, final Duels api, final Arena arena, File file) throws IOException {
        this.api = api;
        this.extension = extension;
        this.handler = extension.getHandler();
        this.config = extension.getConfiguration();
        this.arena = arena;
        this.file = file;
        this.logger = extension.getApi().getLogger();

        // Convert from old yml format if needed
        if (file.getName().endsWith(".yml")) {
            final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            final File newFile = new File(file.getParent(), arena.getName() + ".txt");

            try (final BufferedWriter writer = new BufferedWriter(new FileWriter(newFile))) {
                writer.write(config.getString("world"));
                writer.newLine();
                writer.write(String.valueOf(config.getInt("min.x")));
                writer.newLine();
                writer.write(String.valueOf(config.getInt("min.y")));
                writer.newLine();
                writer.write(String.valueOf(config.getInt("min.z")));
                writer.newLine();
                writer.write(String.valueOf(config.getInt("max.x")));
                writer.newLine();
                writer.write(String.valueOf(config.getInt("max.y")));
                writer.newLine();
                writer.write(String.valueOf(config.getInt("max.z")));
                writer.newLine();

                final ConfigurationSection blocks = config.getConfigurationSection("blocks");

                if (blocks == null) {
                    return;
                }

                for (String key : blocks.getKeys(false)) {
                    writer.write(key + ":" + blocks.getString(key));
                    writer.newLine();
                }
            }

            file.delete();
            extension.info("Converted " + file.getName() + " to " + newFile.getName() + ".");

            this.file = file = newFile;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String worldName = reader.readLine();
            final World world;

            if (worldName == null || (world = Bukkit.getWorld(worldName)) == null) {
                throw new NullPointerException("worldName or world is null");
            }

            this.min = new Location(
                    world,
                    Integer.parseInt(reader.readLine()),
                    Integer.parseInt(reader.readLine()),
                    Integer.parseInt(reader.readLine()));
            this.max = new Location(
                    world,
                    Integer.parseInt(reader.readLine()),
                    Integer.parseInt(reader.readLine()),
                    Integer.parseInt(reader.readLine()));
        }

        if (!config.isTrackBlockChanges()) {
            this.blocks = loadBlocks();
        }

        loadChunks();
    }

    private Map<Position, BlockInfo> loadBlocks() throws IOException {
        final Map<Position, BlockInfo> blocks = new HashMap<>();

        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Skip 7 lines to get to blocks section
            for (int i = 0; i < 7; i++) {
                reader.readLine();
            }

            String block;

            while ((block = reader.readLine()) != null) {
                final String[] data = block.split(":");
                final String[] posData = data[0].split(";");
                final Position pos = new Position(
                        Integer.parseInt(posData[0]), Integer.parseInt(posData[1]), Integer.parseInt(posData[2]));
                final String[] blockData = data[1].split(";");
                final BlockInfo info = new BlockInfo(Material.getMaterial(blockData[0]), Byte.parseByte(blockData[1]));
                blocks.put(pos, info);
            }
        }

        return blocks;
    }

    private void loadChunks() {
        for (int x = min.getBlockX() >> 4; x <= max.getBlockX() >> 4; x++) {
            for (int z = min.getBlockZ() >> 4; z <= max.getBlockZ() >> 4; z++) {
                chunks.add(new ChunkLoc(x, z));
            }
        }
    }

    public String getName() {
        return arena.getName();
    }

    private int calculateSize() {
        return (max.getBlockX() - min.getBlockX() + 1)
                + (max.getBlockY() - min.getBlockY() + 1)
                + (max.getBlockZ() - min.getBlockZ() + 1);
    }

    public int getTotalBlocks() {
        return config.isTrackBlockChanges() ? calculateSize() : blocks.size();
    }

    public World getWorld() {
        return min.getWorld();
    }

    void delete() {
        file.delete();
    }

    public boolean isResetting() {
        return task != null;
    }

    // Called before reset zones are saved to files.
    public void resetInstant() throws IOException {
        final Map<Position, BlockInfo> blocks = loadBlocks();

        BlockUtil.runForCuboid(min, max, block -> {
            final Position pos = new Position(block);
            final BlockInfo info = blocks.get(pos);

            if (info == null) {
                if (block.getType() != Material.AIR) {
                    handler.setBlockFast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), 0, Material.AIR);
                }

                return;
            } else if (info.matches(block)) {
                return;
            }

            handler.setBlockFast(getWorld(), pos.getX(), pos.getY(), pos.getZ(), info.getData(), info.getType());
            handler.updateLighting(getWorld(), pos.getX(), pos.getY(), pos.getZ());
        });
    }

    public void startSyncTaskTimer(final Task task) {
        this.task = task;

        if (task != null) {
            task.runTaskTimer(api, 1L, 1L);
        }
    }

    public void startAsyncTask(final Task task) {
        this.task = task;

        if (task != null) {
            task.runTaskAsynchronously(api);
        }
    }

    public void reset(final Callback onDone) {
        arena.setDisabled(true);

        if (!config.isTrackBlockChanges()) {
            this.logger.info("Starting to scan zone " + this.getName());
            startSyncTaskTimer(new ScanBlocksTask(this.logger, extension, this, onDone));
            return;
        }

        this.logger.info("Starting to filter zone " + this.getName());
        startAsyncTask(new FilterBlocksTask(this.logger, extension, this, onDone, this.changedBlocks));
        this.changedBlocks = new HashSet<>();
    }

    public void reset() {
        reset(null);
    }

    public boolean contains(final Location location) {
        return min.getWorld().equals(location.getWorld())
                && min.getBlockX() <= location.getBlockX()
                && location.getBlockX() <= max.getBlockX()
                && min.getBlockY() <= location.getBlockY()
                && location.getBlockY() <= max.getBlockY()
                && min.getBlockZ() <= location.getBlockZ()
                && location.getBlockZ() <= max.getBlockZ();
    }

    public boolean contains(final Block block) {
        return contains(block.getLocation());
    }

    public boolean contains(final Chunk chunk) {
        return chunks.contains(new ChunkLoc(chunk));
    }

    public boolean isCached(final Block block) {
        return contains(block) && blocks.containsKey(new Position(block));
    }

    public void track(final Block block) {
        changedBlocks.add(new Position(block));
    }

    public void track(final Collection<Block> blocks) {
        blocks.forEach(this::track);
    }
}
