package com.doom.platform;

import java.util.Locale;

public final class EnvironmentDetector {

    private static final boolean ANDROID = detectAndroid();

    private EnvironmentDetector() {}

    public static OSFamily getOSFamily() {
        if (isAndroid()) return OSFamily.Android;
        if (isWindows()) return OSFamily.Windows;
        if (isMac()) return OSFamily.MacOS;
        if (isLinuxDesktop()) return OSFamily.Linux;
        return OSFamily.Unknown;
    }

    public static String getRawOsName() {
        return System.getProperty("os.name", "");
    }

    public static String getOsVersion() {
        return System.getProperty("os.version", "");
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean detectAndroid() {
        if (System.getenv("ANDROID_ROOT") != null && System.getenv("ANDROID_DATA") != null) {
            return true;
        }

        try {
            Class.forName("android.os.Build", false,
                    EnvironmentDetector.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static boolean isAndroid() {
        return ANDROID;
    }

    public static boolean isWindows() {
        return !isAndroid() && lower(getRawOsName()).contains("windows");
    }

    public static boolean isMac() {
        if (isAndroid()) return false;

        String os = lower(getRawOsName());
        return os.contains("mac") || os.contains("darwin");
    }

    public static boolean isLinuxDesktop() {
        if (isAndroid()) return false;

        String os = lower(getRawOsName());
        return os.contains("linux") || os.contains("unix")
                || os.contains("freebsd");
    }

    public static String getTrueArchitecture() {
        String arch = lower(System.getProperty("os.arch", "")).trim();

        if (isAndroid()) {
            String normalized = normalizeArchitecture(arch);

            switch (normalized) {
                case "aarch64":
                    return "arm64-v8a";
                case "arm":
                    return "armeabi-v7a";
                case "amd64":
                    return "x86_64";
                case "x86":
                    return "x86";
                default:
                    break;
            }

            try {
                Class<?> build = Class.forName("android.os.Build");
                String[] abis = (String[]) build.getField("SUPPORTED_ABIS").get(null);

                if (abis != null && abis.length > 0) {
                    return abis[0].toLowerCase(Locale.ROOT);
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }

            return arch;
        }

        return normalizeArchitecture(arch);
    }

    private static String normalizeArchitecture(String arch) {
        switch (arch) {
            case "x86_64":
            case "x86-64":
            case "amd64":
            case "ia32e":
            case "em64t":
                return "amd64";

            case "x86":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
            case "ia32":
                return "x86";

            case "aarch64":
            case "arm64":
            case "arm64-v8a":
                return "aarch64";

            case "arm":
            case "arm32":
            case "armv7":
            case "armv7l":
            case "armeabi-v7a":
                return "arm";

            default:
                return "unknown".equals(arch) || arch.isEmpty()
                        ? "unknown" : arch;
        }
    }

    public static String getNativeLibraryName() {
        OSFamily os = getOSFamily();
        String arch = getTrueArchitecture();

        switch (os) {
            case Android:
                switch (arch) {
                    case "arm64-v8a": return "android-arm64";
                    case "armeabi-v7a": return "android-arm32";
                    case "x86_64": return "android-x64";
                    case "x86": return "android-x86";
                    default: return null;
                }

            case Windows:
                if ("amd64".equals(arch)) return "windows-x64";
                if ("x86".equals(arch)) return "windows-x86";
                if ("aarch64".equals(arch)) return "windows-arm64";
                return null;

            case Linux:
                if ("amd64".equals(arch)) return "linux-x64";
                if ("x86".equals(arch)) return "linux-x86";
                if ("aarch64".equals(arch)) return "linux-arm64";
                if ("arm".equals(arch)) return "linux-arm32";
                return null;

            case MacOS:
                if ("aarch64".equals(arch)) return "macos-arm64";
                if ("amd64".equals(arch)) return "macos-x64";
                return null;

            default:
                return null;
        }
    }

    public static String getJvmBitness() {
        String model = System.getProperty("sun.arch.data.model");

        if ("32".equals(model) || "64".equals(model)) {
            return model;
        }

        String arch = lower(System.getProperty("os.arch", ""));
        return arch.contains("64") || arch.equals("aarch64")
                || arch.equals("arm64") ? "64" : "32";
    }
}