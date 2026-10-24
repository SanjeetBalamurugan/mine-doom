package com.doom;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.doom.utils.io;

public class WadHelper {
    private static final String[] wad_names = {
        "doom2.wad",
        "doomu.wad",
        "doom.wad",
        "doom1.wad",
        "plutonia.wad",
        "tnt.wad",
        "doom2f.wad"
    };

    private static final Pattern MAP_LUMP = Pattern.compile("^(E[1-9]M[1-9]|MAP(0[1-9]|[1-9][0-9]))$");

    /*
    * Returns wad file
    */
    public static Path getWadDirectory(Path gameDir) {
        Path dir = gameDir.resolve("config").resolve("minedoom");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.out.println("[MineDoom] Could not create WAD folder at: " + dir.toAbsolutePath());
        }
        return dir;
    }

    public static List<Path> findWads(Path gameDir) {
        Path dir = getWadDirectory(gameDir);
        List<Path> found = new ArrayList<>();

        for (String wad : wad_names) {
            Path wadPath = dir.resolve(wad);
            if (Files.exists(wadPath) && isValidWad(wadPath.toFile())) {
                found.add(wadPath);
            }
        }

        return found;
    }

    public static List<String> findInvalidWads(Path gameDir) {
        Path dir = getWadDirectory(gameDir);
        List<String> invalid = new ArrayList<>();

        for (String wad : wad_names) {
            Path wadPath = dir.resolve(wad);
            if (Files.exists(wadPath) && !isValidWad(wadPath.toFile())) {
                invalid.add(wad);
            }
        }

        return invalid;
    }

    public static Path pick(List<Path> wads, String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        for (Path wad : wads) {
            if (wad.getFileName().toString().equalsIgnoreCase(fileName)) {
                return wad;
            }
        }
        return null;
    }

    public static Path getSaveDirectory(Path gameDir, Path wad) {
        String name = wad.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isEmpty()) name = "default";
        return gameDir.resolve("config").resolve("minedoom").resolve("saves").resolve(name);
    }

    public static Path getSelectedWad(Path gameDir, String lastWad) {
        List<Path> wads = findWads(gameDir);
        Path chosen = pick(wads, lastWad);
        if (chosen != null) return chosen;
        return wads.size() == 1 ? wads.get(0) : null;
    }

    public static Path getWadFile(Path gameDir) {
        List<Path> wads = findWads(gameDir);
        return wads.isEmpty() ? null : wads.get(0);
    }

    public static String displayName(String fileName) {
        return switch (fileName.toLowerCase(Locale.ROOT)) {
            case "doom2.wad" -> "Doom II: Hell on Earth";
            case "doomu.wad" -> "The Ultimate Doom";
            case "doom.wad" -> "Doom (Registered)";
            case "doom1.wad" -> "Doom (Shareware)";
            case "plutonia.wad" -> "The Plutonia Experiment";
            case "tnt.wad" -> "TNT: Evilution";
            case "doom2f.wad" -> "Doom II (French)";
            default -> fileName;
        };
    }

    public static Path prepareActiveDirectory(Path gameDir, Path wad) throws IOException {
        String name = wad.getFileName().toString();
        String folder = name.toLowerCase(Locale.ROOT).replaceFirst("\\.wad$", "");
        Path active = getWadDirectory(gameDir).resolve(".active").resolve(folder);
        Files.createDirectories(active);

        Path target = active.resolve(name);
        if (Files.exists(target)
                && Files.size(target) == Files.size(wad)
                && Files.getLastModifiedTime(target).equals(Files.getLastModifiedTime(wad))) {
            return active;
        }

        Files.deleteIfExists(target);
        try {
            Files.createLink(target, wad);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            Files.copy(wad, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }

        return active;
    }

    public static List<String> getMapLumps(Path wadPath) {
        List<String> maps = new ArrayList<>();
        if (wadPath == null) return maps;

        File wad = wadPath.toFile();
        if (!isValidWad(wad)) return maps;

        try (RandomAccessFile raf = new RandomAccessFile(wad, "r")) {
            raf.seek(4);
            int numLumps = io.readInt32LE(raf);
            int directoryOffset = io.readInt32LE(raf);

            if (numLumps <= 0 || numLumps > 200000) return maps;
            if ((long) directoryOffset + (long) numLumps * 16L > wad.length()) return maps;

            raf.seek(directoryOffset);
            byte[] directory = new byte[numLumps * 16];
            raf.readFully(directory);

            TreeSet<String> found = new TreeSet<>();
            for (int i = 0; i < numLumps; i++) {
                int start = i * 16 + 8;
                int end = start;
                while (end < start + 8 && directory[end] != 0) end++;

                String name = new String(directory, start, end - start, StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT);
                if (MAP_LUMP.matcher(name).matches()) {
                    found.add(name);
                }
            }
            maps.addAll(found);
        } catch (IOException e) {
            System.out.println("[MineDoom] Failed to read map list from " + wad.getName());
        }

        return maps;
    }

    public static boolean isValidWad(File wad) {
        if (wad == null || !wad.exists() || wad.length() < 12) {
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile(wad, "r")) {
            byte[] sigBytes = new byte[4];
            raf.readFully(sigBytes);
            String signature = new String(sigBytes, StandardCharsets.US_ASCII);

            if (!signature.equals("IWAD") && !signature.equals("PWAD")) {
                return false;
            }

            int numLumps = io.readInt32LE(raf);
            int directoryOffset = io.readInt32LE(raf);

            if (directoryOffset < 12 || directoryOffset > wad.length()) {
                return false;
            }

            return numLumps >= 0;
        } catch (IOException e) {
            return false;
        }
    }
}
