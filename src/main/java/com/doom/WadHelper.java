package com.doom;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.doom.utils.io;

public class WadHelper {
    private static final String[] wad_names = {
        "doom2.wad",    // Commercial
        "doomu.wad",    // Retail
        "doom.wad",     // Registered
        "doom1.wad",    // Shareware
        "plutonia.wad", // hmm
        "tnt.wad",      // hmm
        "doom2f.wad"    // "French stuff"
    };

    /*
    * Returns the wad file in priority or "unknown" if none exists
    */
    public static Path getWadFile(Path path) throws IOException {
        List<Path> found = new ArrayList<>();

        for (String wad : wad_names) {
            Path wadPath = path.resolve("config/minedoom/" + wad);
            if (!Files.exists(wadPath)) {
                System.out.println("[MineDoom] " + wad + " DOES NOT EXIST at path: " + wadPath.toAbsolutePath());
                continue;
            }

            if (isValidWad(wadPath.toFile())) {
                found.add(wadPath);
            } else {
                System.out.println("[MineDoom] " + wad + " exists but failed headers / validation check.");
            }
        }

        if (found.isEmpty()) {
            throw new IOException("Cannot find any suitable wad file");
        }

        return found.get(0);
    }


    public static boolean isValidWad(File wad)
    {
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

            if (numLumps < 0) {
                return false;
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
