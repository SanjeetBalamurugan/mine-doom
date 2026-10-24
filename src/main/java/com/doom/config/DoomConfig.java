package com.doom.config;

import com.doom.DoomMaps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DoomConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("DoomConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String[] SKILL_NAMES = {
        "Too Young to Die",
        "Not Too Rough",
        "Hurt Me Plenty",
        "Ultra-Violence",
        "Nightmare"
    };

    private static DoomConfig instance = new DoomConfig();
    private static Path file;
    private static boolean loaded = false;

    public String lastWad = "";
    public boolean soundEnabled = true;
    public int soundVolume = 100;
    public int windowScale = 100;
    public boolean roundedCorners = true;
    public boolean showFps = false;
    public String startMap = "";
    public int skill = 3;
    public boolean noMonsters = false;
    public boolean respawn = false;
    public boolean fastMonsters = false;

    public static DoomConfig get() {
        return instance;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static synchronized DoomConfig load(Path gameDir) {
        file = gameDir.resolve("config").resolve("minedoom").resolve("minedoom.json");
        DoomConfig result = null;

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                result = GSON.fromJson(reader, DoomConfig.class);
            } catch (IOException | JsonParseException e) {
                LOGGER.warn("Failed to read {}, using defaults", file, e);
            }
        }

        instance = result != null ? result : new DoomConfig();
        instance.validate();
        loaded = true;
        instance.save();
        return instance;
    }

    public synchronized void save() {
        if (file == null) return;
        validate();

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Failed to save {}", file, e);
        }
    }

    public void validate() {
        lastWad = lastWad == null ? "" : lastWad.trim();
        soundVolume = Math.max(0, Math.min(100, soundVolume));
        windowScale = Math.max(30, Math.min(100, windowScale));
        skill = skill < 1 || skill > 5 ? 3 : skill;
        startMap = startMap == null ? "" : startMap.trim().toUpperCase(Locale.ROOT);
    }

    public void reset() {
        DoomConfig d = new DoomConfig();
        soundEnabled = d.soundEnabled;
        soundVolume = d.soundVolume;
        windowScale = d.windowScale;
        roundedCorners = d.roundedCorners;
        showFps = d.showFps;
        startMap = d.startMap;
        skill = d.skill;
        noMonsters = d.noMonsters;
        respawn = d.respawn;
        fastMonsters = d.fastMonsters;
    }

    public List<String> buildEngineArgs(List<String> availableMaps) {
        List<String> args = new ArrayList<>();

        if (!startMap.isEmpty() && availableMaps.contains(startMap)) {
            args.add("-skill");
            args.add(String.valueOf(skill));
            args.addAll(DoomMaps.warpArgs(startMap));
        }

        if (noMonsters) args.add("-nomonsters");
        if (respawn) args.add("-respawn");
        if (fastMonsters) args.add("-fast");
        return args;
    }

    public static String skillLabel(int skill) {
        return SKILL_NAMES[Math.max(1, Math.min(SKILL_NAMES.length, skill)) - 1];
    }
}
