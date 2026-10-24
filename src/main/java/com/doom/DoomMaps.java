package com.doom;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DoomMaps {
    private static final Pattern EPISODE_MAP = Pattern.compile("^E([1-9])M([1-9])$");
    private static final Pattern COMMERCIAL_MAP = Pattern.compile("^MAP(\\d\\d)$");

    private static final String[][] EPISODES = {
        {"Hangar", "Nuclear Plant", "Toxin Refinery", "Command Control", "Phobos Lab",
         "Central Processing", "Computer Station", "Phobos Anomaly", "Military Base"},
        {"Deimos Anomaly", "Containment Area", "Refinery", "Deimos Lab", "Command Center",
         "Halls of the Damned", "Spawning Vats", "Tower of Babel", "Fortress of Mystery"},
        {"Hell Keep", "Slough of Despair", "Pandemonium", "House of Pain", "Unholy Cathedral",
         "Mt. Erebus", "Limbo", "Dis", "Warrens"},
        {"Hell Beneath", "Perfect Hatred", "Sever the Wicked", "Unruly Evil", "They Will Repent",
         "Against Thee Wickedly", "And Hell Followed", "Unto the Cruel", "Fear"}
    };

    private static final String[] DOOM2 = {
        "Entryway", "Underhalls", "The Gantlet", "The Focus", "The Waste Tunnels", "The Crusher",
        "Dead Simple", "Tricks and Traps", "The Pit", "Refueling Base", "Circle of Death",
        "The Factory", "Downtown", "The Inmost Dens", "Industrial Zone", "Suburbs", "Tenements",
        "The Courtyard", "The Citadel", "Gotcha!", "Nirvana", "The Catacombs", "Barrels o' Fun",
        "The Chasm", "Bloodfalls", "The Abandoned Mines", "Monster Condo", "The Spirit World",
        "The Living End", "Icon of Sin", "Wolfenstein", "Grosse"
    };

    private DoomMaps() {}

    public static String title(String lump, String wadFileName) {
        Matcher m = EPISODE_MAP.matcher(lump);
        if (m.matches()) {
            int ep = Integer.parseInt(m.group(1)) - 1;
            int map = Integer.parseInt(m.group(2)) - 1;
            if (ep < EPISODES.length && map < EPISODES[ep].length) {
                return EPISODES[ep][map];
            }
            return null;
        }

        m = COMMERCIAL_MAP.matcher(lump);
        if (m.matches() && "doom2.wad".equalsIgnoreCase(wadFileName)) {
            int map = Integer.parseInt(m.group(1)) - 1;
            if (map >= 0 && map < DOOM2.length) {
                return DOOM2[map];
            }
        }
        return null;
    }

    public static String label(String lump, String wadFileName) {
        String title = title(lump, wadFileName);
        return title == null ? lump : lump + " - " + title;
    }

    public static List<String> warpArgs(String lump) {
        Matcher m = EPISODE_MAP.matcher(lump);
        if (m.matches()) {
            return List.of("-warp", m.group(1), m.group(2));
        }

        m = COMMERCIAL_MAP.matcher(lump);
        if (m.matches()) {
            return List.of("-warp", String.valueOf(Integer.parseInt(m.group(1))));
        }
        return List.of();
    }
}
