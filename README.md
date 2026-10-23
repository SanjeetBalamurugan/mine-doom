# MineDoom

**MineDoom** runs id Software's original 1997 Linux Doom C release (`linuxdoom-1.10`) natively inside Minecraft's own process via JNI. No virtual machines, no external emulators, and no pre-recorded video—just the real 1997 C engine compiled as a shared library and stepped directly by Minecraft.

---

## Key Features

* **In-Process 1997 C Engine:** Compiled as a native shared library (`minedoom.dll` / `libminedoom.so`) bundled inside the mod JAR and loaded at runtime.

* **Authentic PLAYPAL & Gamma Support:** Captures all 14 hardware colour palettes (damage reds 1–8, item pickup yellows 9–12, radiation suit green 13) plus `F11` gamma correction levels.

* **Crash & Exit Isolation (`setjmp` / `longjmp`):** Picking *Quit Game* * or hitting an internal Doom `I_Error` jumps back across the C call stack cleanly without ever closing Minecraft or losing your world.

---

## Requirements & Supported Platforms

| Requirement | Version / Detail |

| :--- | :--- |

| **Minecraft** | `26.3` (Java Edition) |

| **Mod Loader** | Fabric Loader `>= 0.19.5` + Fabric API `>= 0.162.0+26.3` |

| **Java** | Java `25` |

---

## Installation & WAD Setup

1. Install **Fabric Loader** (`0.19.5+`) and **Fabric API** (`0.162.0+26.3`) for **Minecraft 26.3**.

2. Drop `minedoom-1.0.0.jar` into your `.minecraft/mods/` folder.

3. **Supply a WAD File (Required):** MineDoom does **not** bundle copyrighted game data.

   * Launch Minecraft, place a **Doom Doom Block**, and right-click it.

   * Click **Open WAD Folder** on the prompt screen (or navigate to `.minecraft/minedoom/`).

   * Place `doom1.wad` (the freely distributable Episode 1 Shareware WAD), `doom.wad`, or `doom2.wad` into the `minedoom/` folder.

   * Click **Check Again** (or reopen the cabinet) to boot straight into Doom without restarting Minecraft.

---

## Why the 1997 Source Needed Fixes to Build
Cloning the untouched December 1997 release under modern C23 / 64-bit compilers produces over 500 errors (implicit int, removed headers like <errnos.h>, true/false keyword collisions, empty-parenthesis function pointer mismatches in info.c, and 64-bit pointer-to-int truncations in m_misc.c, r_data.c, and p_saveg.c).
<!-- Full reproduction scripts and compiler logs are documented in docs/. -->

# Building from Source
## 1. Clone the repository
``` text
git clone https://github.com/SanjeetBalamurugan/mine-doom
cd mine-doom
```

## 2. Download all dependencies
``` text
chmod +x scripts/linux.sh scripts/install-toolchains.sh
scripts/linux.sh
scripts/install-toolchains.sh
```

## 3. Build the unified Fabric mod JAR
``` text
chmod +x scripts/build-all.sh -- Builds the headless doom to native libraries
scripts/build-all.sh

./gradlew build -- automatically package the native libs within the jar
```

# Credits & Acknowledgements

* **id Software** — For creating DOOM (1993), releasing the Linux Doom 1.10 source code in December 1997, and re-licensing it under the GPL in October 1999.
