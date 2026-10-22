package com.doom;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.doom.platform.EnvironmentDetector;
import com.doom.platform.OSFamily;

public class DoomJNI {
    private static boolean loaded = false;
    private static Logger LOGGER = LoggerFactory.getLogger("DoomJNI");

    public DoomJNI(Logger instance)
    {
        if (instance != null) LOGGER = instance;
    }

    public boolean isLoaded()
    {
        return loaded;
    }

    static
    {
        try
        {
            if (EnvironmentDetector.getOSFamily() == OSFamily.Unknown)
                throw new IllegalStateException(EnvironmentDetector.getRawOsName() + " is not a supported os");

            String platformDirectory = getPlatformDirectory();
            if (platformDirectory == null)
                throw new IllegalStateException("No native build for " + EnvironmentDetector.getRawOsName()
                        + " " + EnvironmentDetector.getTrueArchitecture());

            String libName = System.mapLibraryName("doomjni");
            String resourcePath = "/native/" + platformDirectory + "/" + libName;

            LOGGER.info("DOOMJNI: Trying to load " + resourcePath);

            try (InputStream libStream = DoomJNI.class.getResourceAsStream(resourcePath))
            {
                if (libStream == null) throw new RuntimeException("Native library not found: " + resourcePath);
                Path temp = Files.createTempFile("doomjni-", platformDirectory + "-" + libName);
                temp.toFile().deleteOnExit();
                Files.copy(libStream, temp, StandardCopyOption.REPLACE_EXISTING);
                System.load(temp.toAbsolutePath().toString());
            }
            loaded = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Doom JNI library", e);
        }
    }

    private static String getPlatformDirectory()
    {
        return EnvironmentDetector.getNativeLibraryName();
    }

    public static native void init(String[] args);
    public static native void runTick();
    public static native boolean isPlayerReady();
    public static native int getGameTic();
    public static native int getLevelTime();
    public static native int getScreenWidth();
    public static native int getScreenHeight();
    public static native byte[] getScreenBuffer();
    public static native void renderFrame();
    public static native byte[] getPalette();
    public static native void keyDown(int key);
    public static native void keyUp(int key);
    public static native void mouseMove(int x, int y);
    public static native void mouseButton(int button, boolean pressed);
    public static native void sendKeyPress(int key);
    public static native short[] drainSound();

    public static final int KEY_RIGHTARROW = 0xae;
    public static final int KEY_LEFTARROW = 0xac;
    public static final int KEY_UPARROW = 0xad;
    public static final int KEY_DOWNARROW = 0xaf;
    public static final int KEY_ESCAPE = 27;
    public static final int KEY_ENTER = 13;
    public static final int KEY_TAB = 9;
    public static final int KEY_F1 = 187;
    public static final int KEY_F2 = 188;
    public static final int KEY_F3 = 189;
    public static final int KEY_F4 = 190;
    public static final int KEY_F5 = 191;
    public static final int KEY_F6 = 192;
    public static final int KEY_F7 = 193;
    public static final int KEY_F8 = 194;
    public static final int KEY_F9 = 195;
    public static final int KEY_F10 = 196;
    public static final int KEY_F11 = 197;
    public static final int KEY_F12 = 198;
    public static final int KEY_BACKSPACE = 127;
    public static final int KEY_PAUSE = 0xff;
    public static final int KEY_EQUALS = 0x3d;
    public static final int KEY_MINUS = 0x2d;
    public static final int KEY_RSHIFT = 0xb6;
    public static final int KEY_RCTRL = 0x9d;
    public static final int KEY_RALT = 0xb8;
}
