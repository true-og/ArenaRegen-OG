package me.realized.de.arenaregen.zone;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import me.realized.de.arenaregen.ArenaRegen;
import me.realized.de.arenaregen.selection.Selection;
import me.realized.duels.api.Duels;
import me.realized.duels.api.arena.Arena;
import me.realized.duels.api.arena.ArenaManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ZoneManager {

    private static final String BUNDLED_ZONES_PREFIX = "zones/";

    private final ArenaRegen extension;
    private final Duels api;
    private final ArenaManager arenaManager;
    private final File folder;

    private final Map<String, Zone> zones = new HashMap<>();

    public ZoneManager(final ArenaRegen extension, final Duels api) {
        this.extension = extension;
        this.api = api;
        this.arenaManager = api.getArenaManager();
        this.folder = new File(extension.getDataFolder(), "zones");
        api.registerListener(new ZoneListener(extension, this));

        final boolean seed = !folder.exists();

        if (seed) {
            folder.mkdirs();
            extractBundledZones();
        }

        final File[] files = folder.listFiles();

        if (files != null) {
            for (final File file : files) {
                final String name = file.getName().substring(0, file.getName().lastIndexOf("."));
                final Arena arena = arenaManager.get(name);
                
                if (arena == null) {
                    file.delete();
                    continue;
                }

                try {
                    zones.put(name, new Zone(extension, api, arena, file));
                } catch (Exception ex) {
                    extension.error("Could not load reset zone '" + name + "'!", ex);
                }
            }
        }
    }

    public void handleDisable() {
        zones.values().stream().filter(zone -> zone.getArena().isUsed() || zone.isResetting()).forEach(zone -> {
            zone.getArena().setDisabled(false);

            if (zone.isResetting()) { 
                zone.getTask().cancel();
            }

            try {
                zone.resetInstant();
            } catch (Exception ex) {
                extension.error("Could not reset zone '" + zone.getName() + "'!", ex);
            }
        });
    }

    public Zone get(final String name) {
        return zones.get(name);
    }

    public Zone get(final Arena arena) {
        return get(arena.getName());
    }

    public Zone get(final Player player) {
        final Arena arena = arenaManager.get(player);
        return arena != null ? get(arena) : null;
    }

    public Zone get(final Block block) {
        return zones.values().stream().filter(any -> any.contains(block)).findFirst().orElse(null);
    }

    public boolean create(final Arena arena, final Selection selection) {
        if (zones.containsKey(arena.getName())) {
            return false;
        }

        final Zone zone = new Zone(extension, api, arena, folder, selection.getFirst(), selection.getSecond());
        zones.put(arena.getName(), zone);
        return true;
    }

    public boolean remove(final String name) {
        final Zone zone = zones.remove(name);

        if (zone == null) {
            return false;
        }

        zone.delete();
        return true;
    }

    public Collection<Zone> getZones() {
        return zones.values();
    }

    private void extractBundledZones() {
        final File jar = extension.getFile();

        if (jar == null || !jar.isFile()) {
            return;
        }

        try (final JarFile jarFile = new JarFile(jar)) {
            final Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final String entryName = entry.getName();

                if (entry.isDirectory() || !entryName.startsWith(BUNDLED_ZONES_PREFIX) || entryName.length() == BUNDLED_ZONES_PREFIX.length()) {
                    continue;
                }

                final String relative = entryName.substring(BUNDLED_ZONES_PREFIX.length());

                if (relative.indexOf('/') >= 0) {
                    continue;
                }

                try {
                    extension.saveResource(entryName);
                } catch (IllegalArgumentException ex) {
                    extension.error("Could not extract bundled zone '" + relative + "'!", ex);
                }
            }
        } catch (IOException ex) {
            extension.error("Could not read bundled zones from extension jar!", ex);
        }
    }
}
