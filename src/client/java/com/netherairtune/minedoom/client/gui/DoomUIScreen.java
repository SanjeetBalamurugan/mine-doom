package com.netherairtune.minedoom.client.gui;

import com.doom.DoomJNI;
import com.doom.WadHelper;
import com.doom.config.DoomConfig;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.NativeImage;
import com.netherairtune.minedoom.client.DoomLog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DoomUIScreen extends Screen {
    private enum Mode { PLAY, NO_WAD, CHOOSE_WAD }

    private static boolean engineInitialized = false;

    private NativeImage currentDoomFrame;
    private DynamicTexture dynamicTexture;
    private Identifier frameLocation;
    private volatile int frameCount = 0;
    private int doomWidth = 320;
    private int doomHeight = 200;

    private int windowWidth = 640;
    private int windowHeight = 400;
    private int footerHeight = 90;
    private int windowX;
    private int windowY;
    private boolean dragging = false;
    private int dragOffsetX;
    private int dragOffsetY;
    private static final float FOOTER_RATIO = 0.14f;
    private static final int FOOTER_BORDER = 2;
    private static final int CORNER_RADIUS = 10;
    private static final int FOOTER_BORDER_COLOR = 0xFF4C9A2A;
    private static final int FOOTER_BG_COLOR = 0xFFD9D9D9;
    private static final int TITLE_COLOR = 0xFFDD0033;
    private static final int SETTINGS_BUTTON_WIDTH = 70;

    private BedrockCloseButton closeButton;
    private Button settingsButton;

    private Mode mode = Mode.PLAY;
    private boolean openFolderFailed = false;
    private boolean reselectWad = false;
    private boolean forceChooser = false;
    private String pendingWad = null;
    private String wadNotice = null;
    private List<Path> availableWads = new ArrayList<>();
    private List<String> invalidWads = new ArrayList<>();
    private Path wadDir;
    private int panelX0;
    private int panelY0;
    private int panelX1;
    private int panelY1;

    private int fps = 0;
    private int fpsLastCount = 0;
    private long fpsLastNs = System.nanoTime();

    private Thread doomEngineThread;
    private SourceDataLine audioLine;
    private volatile boolean isRunning = true;
    private volatile boolean paused = false;
    private volatile boolean frameReady = false;
    private volatile long lastHeartbeat = System.nanoTime();
    private volatile long quitKeyAt = 0L;
    private boolean closing = false;
    private boolean suspended = false;
    private final Object renderLock = new Object();
    private final ConcurrentLinkedQueue<Runnable> inputQueue = new ConcurrentLinkedQueue<>();
    private final Set<Integer> heldKeys = ConcurrentHashMap.newKeySet();

    public DoomUIScreen() {
        super(Component.translatable("minedoom.screen.title"));
    }

    public void requestWadChange() {
        this.reselectWad = true;
    }

    @Override
    protected void init() {
        super.init();

        this.paused = false;
        this.mode = Mode.PLAY;
        this.openFolderFailed = false;
        Path mcDir = this.minecraft.gameDirectory.toPath();
        this.wadDir = WadHelper.getWadDirectory(mcDir);

        if (!DoomConfig.isLoaded()) {
            DoomConfig.load(mcDir);
        }
        DoomConfig cfg = DoomConfig.get();

        if (this.reselectWad) {
            this.reselectWad = false;
            if (stopEngine()) {
                Minecraft.getInstance().execute(this::onClose);
                return;
            }
            resetEngineState();
            this.forceChooser = true;
        }

        try {
            if (!engineInitialized) {
                this.availableWads = WadHelper.findWads(mcDir);
                this.invalidWads = WadHelper.findInvalidWads(mcDir);
                String wanted = this.pendingWad != null ? this.pendingWad : cfg.lastWad;

                if (this.availableWads.isEmpty()) {
                    this.mode = Mode.NO_WAD;
                    buildMissingWadPrompt();
                    return;
                }

                Path selected = null;
                if (!this.forceChooser) {
                    selected = WadHelper.pick(this.availableWads, wanted);
                    if (selected == null && wanted.isEmpty() && this.availableWads.size() == 1) {
                        selected = this.availableWads.get(0);
                    }
                }

                if (selected == null) {
                    if (this.forceChooser) {
                        this.wadNotice = null;
                    } else if (!wanted.isEmpty()) {
                        this.wadNotice = wanted + " not found or invalid. Pick another WAD.";
                    } else {
                        this.wadNotice = "Multiple WADs found. Pick one to play.";
                    }
                    this.mode = Mode.CHOOSE_WAD;
                    buildWadChooser();
                    return;
                }

                Path iwadDir = selected.getParent();
                if (this.availableWads.size() > 1) {
                    try {
                        iwadDir = WadHelper.prepareActiveDirectory(mcDir, selected);
                    } catch (IOException e) {
                        DoomLog.error(e);
                        this.wadNotice = "Could not prepare " + selected.getFileName() + ".";
                        this.mode = Mode.CHOOSE_WAD;
                        buildWadChooser();
                        return;
                    }
                }

                String iwadPath = iwadDir.toAbsolutePath().toString();
                System.out.println("[MineDoom] Initializing DOOM engine with IWAD path: " + iwadPath);

                List<String> args = new ArrayList<>();
                args.add("-iwad");
                args.add(iwadPath);
                try {
                    Path saveDir = WadHelper.getSaveDirectory(mcDir, selected);
                    java.nio.file.Files.createDirectories(saveDir);
                    args.add("-savedir");
                    args.add(saveDir.toAbsolutePath().toString());
                } catch (IOException e) {
                    DoomLog.error(e);
                }

                args.addAll(cfg.buildEngineArgs(WadHelper.getMapLumps(selected)));

                DoomJNI.init(args.toArray(new String[0]));
                engineInitialized = true;

                cfg.lastWad = selected.getFileName().toString();
                cfg.save();
                this.pendingWad = null;
                this.forceChooser = false;
            }
        } catch (UnsatisfiedLinkError e) {
            DoomLog.error("minedoom.screen.native_missing");
        } catch (Exception e) {
            DoomLog.error(e);
        }

        if (engineInitialized) {
            this.doomWidth = DoomJNI.getScreenWidth();
            this.doomHeight = DoomJNI.getScreenHeight();
        }

        int maxW = this.width - 40;
        int maxH = this.height - 40;
        float scale = Math.min((float) maxW / doomWidth, (float) maxH / (doomHeight + doomWidth * FOOTER_RATIO));
        scale *= cfg.windowScale / 100f;
        windowWidth = (int) (doomWidth * scale);
        windowHeight = (int) (doomHeight * scale);
        footerHeight = (int) (windowWidth * FOOTER_RATIO);
        windowX = (this.width - windowWidth) / 2;
        windowY = (this.height - windowHeight - footerHeight) / 2;

        if (this.currentDoomFrame == null) {
            this.currentDoomFrame = new NativeImage(NativeImage.Format.RGBA, doomWidth, doomHeight, false);
        }

        if (this.dynamicTexture == null) {
            this.dynamicTexture = new DynamicTexture(null, this.currentDoomFrame);
            this.frameLocation = Identifier.fromNamespaceAndPath("minedoom", "doom_render_frame");
            this.minecraft.getTextureManager().register(this.frameLocation, this.dynamicTexture);
        }

        this.closeButton = this.addRenderableWidget(new BedrockCloseButton(0, 0, () -> this.onClose()));
        this.settingsButton = this.addRenderableWidget(Button.builder(Component.literal("Settings"), b -> openSettings())
                .bounds(0, 0, SETTINGS_BUTTON_WIDTH, 20).build());
        layoutButtons();

        startDoomEngine();
    }

    private void buildMissingWadPrompt() {
        int cx = this.width / 2;
        int top = this.height / 2 + 10;

        this.addRenderableWidget(Button.builder(Component.literal("Open WAD Folder"), b -> openWadFolder())
                .bounds(cx - 100, top, 200, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Check Again"), b -> this.rebuildWidgets())
                .bounds(cx - 100, top + 24, 200, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(cx - 100, top + 48, 200, 20).build());
    }

    private void buildWadChooser() {
        DoomConfig cfg = DoomConfig.get();
        int w = 300;
        int rowH = 24;
        int n = this.availableWads.size();
        int h = 36 + n * rowH + 4 + 20 + 10;
        int cx = this.width / 2;
        int top = Math.max(4, (this.height - h) / 2);

        this.panelX0 = cx - w / 2 - 10;
        this.panelX1 = cx + w / 2 + 10;
        this.panelY0 = top;
        this.panelY1 = top + h;

        int y = top + 36;
        for (Path wad : this.availableWads) {
            String file = wad.getFileName().toString();
            String label = WadHelper.displayName(file) + " [" + file + "]"
                    + (file.equalsIgnoreCase(cfg.lastWad) ? " (last)" : "");
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> pickWad(wad))
                    .bounds(cx - w / 2, y, w, 20).build());
            y += rowH;
        }

        y += 4;
        int half = (w - 4) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Open WAD Folder"), b -> openWadFolder())
                .bounds(cx - w / 2, y, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(cx - w / 2 + half + 4, y, half, 20).build());
    }

    private void pickWad(Path wad) {
        this.pendingWad = wad.getFileName().toString();
        this.forceChooser = false;
        this.rebuildWidgets();
    }

    private void openWadFolder() {
        if (this.wadDir == null) return;
        try {
            Blaze3D.openPath(this.wadDir);
            this.openFolderFailed = false;
        } catch (Exception e) {
            this.openFolderFailed = true;
            DoomLog.error(e);
        }
    }

    private void openSettings() {
        for (int key : heldKeys) {
            inputQueue.add(() -> DoomJNI.keyUp(key));
        }
        heldKeys.clear();
        this.paused = true;
        this.suspended = true;
        this.minecraft.gui.setScreen(new DoomSettingsScreen(this));
    }

    private void layoutButtons() {
        int footerY = windowY + windowHeight;
        int buttonY = footerY + (footerHeight - 20) / 2;
        int closeX = windowX + windowWidth - 24;
        closeButton.setPosition(closeX, buttonY);
        settingsButton.setPosition(closeX - 4 - SETTINGS_BUTTON_WIDTH, buttonY);
    }

    private boolean isOutsideRoundedTop(int x, int y) {
        if (y >= CORNER_RADIUS) return false;
        int cx;
        if (x < CORNER_RADIUS) {
            cx = CORNER_RADIUS;
        } else if (x >= doomWidth - CORNER_RADIUS) {
            cx = doomWidth - CORNER_RADIUS;
        } else {
            return false;
        }
        float dx = cx - x - 0.5f;
        float dy = CORNER_RADIUS - y - 0.5f;
        if (x >= doomWidth - CORNER_RADIUS) dx = x - cx + 0.5f;
        return dx * dx + dy * dy > CORNER_RADIUS * CORNER_RADIUS;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (mode != Mode.PLAY || closing || suspended || doomEngineThread == null) return;

        long now = System.nanoTime();
        boolean dead = !doomEngineThread.isAlive();
        boolean stalled = quitKeyAt != 0L
                && now - quitKeyAt < 5_000_000_000L
                && now - lastHeartbeat > 600_000_000L;

        if (dead || stalled) {
            closing = true;
            onClose();
        }
    }

    private boolean stopEngine() {
        this.isRunning = false;

        boolean stuck = false;
        try {
            if (this.doomEngineThread != null) {
                this.doomEngineThread.join(500);
                stuck = this.doomEngineThread.isAlive();
            }
        } catch (InterruptedException ignored) {}

        if (this.audioLine != null) {
            this.audioLine.stop();
            this.audioLine.close();
            this.audioLine = null;
        }

        if (engineInitialized) {
            engineInitialized = false;
            if (!stuck) DoomJNI.shutdown();
        }

        return stuck;
    }

    private void resetEngineState() {
        this.doomEngineThread = null;
        this.isRunning = true;
        this.frameReady = false;
        this.quitKeyAt = 0L;
        this.closing = false;
        this.heldKeys.clear();
        this.inputQueue.clear();
    }

    private void startDoomEngine() {
        if (this.doomEngineThread != null || !engineInitialized) return;

        try {
            AudioFormat format = new AudioFormat(11025f, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            this.audioLine = (SourceDataLine) AudioSystem.getLine(info);
            this.audioLine.open(format);
            this.audioLine.start();
        } catch (Exception e) {
            DoomLog.error(e);
        }

        this.lastHeartbeat = System.nanoTime();
        this.doomEngineThread = new Thread(() -> {
            try {
                while (this.isRunning) {
                    this.lastHeartbeat = System.nanoTime();

                    Runnable task;
                    while ((task = inputQueue.poll()) != null) task.run();

                    if (this.paused) {
                        try { Thread.sleep(1000 / 35); } catch (InterruptedException ignored) {}
                        continue;
                    }

                    DoomConfig cfg = DoomConfig.get();
                    boolean rounded = cfg.roundedCorners;
                    boolean quit = false;

                    synchronized (renderLock) {
                        DoomJNI.runTick();
                        DoomJNI.renderFrame();

                        byte[] pixels = DoomJNI.getScreenBuffer();
                        byte[] palette = DoomJNI.getPalette();

                        if (pixels != null && palette != null && this.currentDoomFrame != null) {
                            for (int y = 0; y < doomHeight; y++) {
                                for (int x = 0; x < doomWidth; x++) {
                                    if (rounded && isOutsideRoundedTop(x, y)) {
                                        this.currentDoomFrame.setPixelABGR(x, y, 0);
                                        continue;
                                    }
                                    int index = pixels[y * doomWidth + x] & 0xFF;
                                    int palIdx = index * 3;
                                    int r = palette[palIdx] & 0xFF;
                                    int g = palette[palIdx + 1] & 0xFF;
                                    int b = palette[palIdx + 2] & 0xFF;
                                    int abgr = (255 << 24) | (b << 16) | (g << 8) | r;
                                    this.currentDoomFrame.setPixelABGR(x, y, abgr);
                                }
                            }

                            frameCount++;
                            frameReady = true;
                        }

                        quit = DoomJNI.hasQuit();
                    }

                    if (quit) {
                        this.isRunning = false;
                        Minecraft.getInstance().execute(this::onClose);
                        break;
                    }

                    SourceDataLine line = this.audioLine;
                    short[] audioBuffer = DoomJNI.drainSound();
                    if (audioBuffer != null && audioBuffer.length > 0 && line != null && cfg.soundEnabled) {
                        int vol = cfg.soundVolume;
                        byte[] byteBuffer = new byte[audioBuffer.length * 2];
                        for (int i = 0; i < audioBuffer.length; i++) {
                            int s = audioBuffer[i] * vol / 100;
                            byteBuffer[i * 2] = (byte) (s & 0xFF);
                            byteBuffer[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
                        }
                        line.write(byteBuffer, 0, byteBuffer.length);
                    }
                    try { Thread.sleep(1000 / 35); } catch (InterruptedException ignored) {}
                }
            } catch (Throwable t) {
                this.isRunning = false;
                DoomLog.error(t);
            }
        });
        this.doomEngineThread.setDaemon(true);
        this.doomEngineThread.start();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (mode == Mode.NO_WAD) {
            int cx = this.width / 2;
            int y = this.height / 2 - 84;
            DoomConfig cfg = DoomConfig.get();

            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("WAD file required").withStyle(ChatFormatting.BOLD));
            lines.add(Component.literal("MineDoom does not bundle copyrighted game data."));
            lines.add(Component.literal("Place doom1.wad, doom.wad, or doom2.wad in:"));
            lines.add(Component.literal(wadDir.toAbsolutePath().toString()).withStyle(ChatFormatting.YELLOW));
            if (!cfg.lastWad.isEmpty()) {
                lines.add(Component.literal("Last used: " + cfg.lastWad + " (not found)").withStyle(ChatFormatting.GRAY));
            }
            if (!invalidWads.isEmpty()) {
                lines.add(Component.literal("Ignored invalid files: " + String.join(", ", invalidWads)).withStyle(ChatFormatting.RED));
            }

            for (Component line : lines) {
                graphics.text(this.font, line, cx - this.font.width(line) / 2, y, 0xFFFFFFFF, false);
                y += 12;
            }

            if (openFolderFailed) {
                Component fail = Component.literal("Could not open the folder. Open it manually.").withStyle(ChatFormatting.RED);
                graphics.text(this.font, fail, cx - this.font.width(fail) / 2, y + 4, 0xFFFFFFFF, false);
            }

            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        if (mode == Mode.CHOOSE_WAD) {
            graphics.fill(panelX0 - 1, panelY0 - 1, panelX1 + 1, panelY1 + 1, FOOTER_BORDER_COLOR);
            graphics.fill(panelX0, panelY0, panelX1, panelY1, 0xF0101010);

            int cx = this.width / 2;
            Component title = Component.literal("Choose a WAD").withStyle(ChatFormatting.BOLD);
            graphics.text(this.font, title, cx - this.font.width(title) / 2, panelY0 + 8, 0xFFFFFFFF, true);

            if (wadNotice != null) {
                Component notice = Component.literal(wadNotice).withStyle(ChatFormatting.YELLOW);
                graphics.text(this.font, notice, cx - this.font.width(notice) / 2, panelY0 + 21, 0xFFFFFF55, false);
            }

            if (openFolderFailed) {
                Component fail = Component.literal("Could not open the folder.").withStyle(ChatFormatting.RED);
                graphics.text(this.font, fail, cx - this.font.width(fail) / 2, panelY1 + 6, 0xFFFFFFFF, false);
            }

            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int footerY = windowY + windowHeight;

        if (!frameReady) {
            Component loading = Component.translatable("minedoom.screen.loading");
            graphics.fill(windowX, windowY, windowX + windowWidth, footerY, 0xFF000000);
            graphics.text(this.font, loading, windowX + (windowWidth - this.font.width(loading)) / 2, windowY + windowHeight / 2, 0xFFFFFFFF, false);
        } else {
            synchronized (renderLock) {
                if (this.dynamicTexture != null) {
                    this.dynamicTexture.upload();
                }
            }

            if (this.frameLocation != null) {
                graphics.blit(this.frameLocation, windowX, windowY, windowX + windowWidth, footerY, 0f, 1f, 0f, 1f);
            }

            if (DoomConfig.get().showFps) {
                long now = System.nanoTime();
                if (now - fpsLastNs >= 1_000_000_000L) {
                    int fc = frameCount;
                    fps = fc - fpsLastCount;
                    fpsLastCount = fc;
                    fpsLastNs = now;
                }
                graphics.text(this.font, Component.literal(fps + " FPS"), windowX + 8, windowY + 8, 0xFFFFFF55, true);
            }
        }

        graphics.fill(windowX, footerY, windowX + windowWidth, footerY + footerHeight, FOOTER_BORDER_COLOR);
        graphics.fill(windowX + FOOTER_BORDER, footerY + FOOTER_BORDER,
                windowX + windowWidth - FOOTER_BORDER, footerY + footerHeight - FOOTER_BORDER,
                FOOTER_BG_COLOR);

        float ts = footerHeight / 15f;
        Component footerTitle = Component
                .literal(Component.translatable("minedoom.screen.title").getString().toUpperCase(Locale.ROOT))
                .withStyle(ChatFormatting.BOLD);
        graphics.pose().pushMatrix();
        graphics.pose().translate(windowX + windowWidth * 0.04f, footerY + (footerHeight - 8 * ts) / 2f);
        graphics.pose().scale(ts, ts);
        graphics.text(this.font, footerTitle, 0, 0, TITLE_COLOR, false);
        graphics.pose().popMatrix();

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (mode != Mode.PLAY) return super.keyPressed(event);
        int doomKey = mapKey(event.key());
        if (doomKey != -1) {
            if (doomKey == 'y') quitKeyAt = System.nanoTime();
            heldKeys.add(doomKey);
            inputQueue.add(() -> DoomJNI.keyDown(doomKey));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (mode != Mode.PLAY) return super.keyReleased(event);
        int doomKey = mapKey(event.key());
        if (doomKey != -1) {
            heldKeys.remove(doomKey);
            inputQueue.add(() -> DoomJNI.keyUp(doomKey));
            return true;
        }
        return super.keyReleased(event);
    }

    private int doomButton(int b) {
        return switch (b) {
            case 1 -> 0;
            case 3 -> 1;
            case 2 -> 2;
            default -> -1;
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
        if (super.mouseClicked(event, doubleClicked)) {
            return true;
        }
        if (mode != Mode.PLAY) return false;

        double x = event.x();
        double y = event.y();
        int footerY = windowY + windowHeight;

        if (event.button() == 1 && x >= windowX && x <= windowX + windowWidth && y >= footerY && y < footerY + footerHeight) {
            dragging = true;
            dragOffsetX = (int) x - windowX;
            dragOffsetY = (int) y - windowY;
            return true;
        }

        if (x >= windowX && x <= windowX + windowWidth && y >= windowY && y <= footerY) {
            int b = doomButton(event.button());
            if (b != -1) inputQueue.add(() -> DoomJNI.mouseButton(b, true));
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (mode != Mode.PLAY) return super.mouseReleased(event);
        if (event.button() == 1) {
            dragging = false;
        }
        int b = doomButton(event.button());
        if (b != -1) inputQueue.add(() -> DoomJNI.mouseButton(b, false));
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            windowX = Math.max(0, Math.min(this.width - windowWidth, (int) event.x() - dragOffsetX));
            windowY = Math.max(0, Math.min(this.height - windowHeight - footerHeight, (int) event.y() - dragOffsetY));
            layoutButtons();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private int mapKey(int k) {
        if (k >= 4 && k <= 29) {
            return switch (k) {
                case 26 -> DoomJNI.KEY_UPARROW;
                case 22 -> DoomJNI.KEY_DOWNARROW;
                case 4 -> DoomJNI.KEY_LEFTARROW;
                case 7 -> DoomJNI.KEY_RIGHTARROW;
                default -> 'a' + (k - 4);
            };
        }
        if (k >= 30 && k <= 38) return '1' + (k - 30);
        if (k == 39) return '0';
        if (k >= 58 && k <= 67) return DoomJNI.KEY_F1 + (k - 58);
        if (k == 68) return DoomJNI.KEY_F11;
        if (k == 69) return DoomJNI.KEY_F12;
        if (k >= 89 && k <= 97) return '1' + (k - 89);
        if (k == 98) return '0';

        return switch (k) {
            case 41 -> DoomJNI.KEY_ESCAPE;
            case 40, 88 -> DoomJNI.KEY_ENTER;
            case 43 -> DoomJNI.KEY_TAB;
            case 42 -> DoomJNI.KEY_BACKSPACE;
            case 44 -> ' ';
            case 45, 86 -> DoomJNI.KEY_MINUS;
            case 46 -> DoomJNI.KEY_EQUALS;
            case 47 -> '[';
            case 48 -> ']';
            case 49 -> '\\';
            case 51 -> ';';
            case 52 -> '\'';
            case 53 -> '`';
            case 54 -> ',';
            case 55, 99 -> '.';
            case 56, 84 -> '/';
            case 72 -> DoomJNI.KEY_PAUSE;
            case 79 -> DoomJNI.KEY_RIGHTARROW;
            case 80 -> DoomJNI.KEY_LEFTARROW;
            case 81 -> DoomJNI.KEY_DOWNARROW;
            case 82 -> DoomJNI.KEY_UPARROW;
            case 85 -> '*';
            case 87 -> '+';
            case 224, 228 -> DoomJNI.KEY_RCTRL;
            case 225, 229 -> DoomJNI.KEY_RSHIFT;
            case 226, 230 -> DoomJNI.KEY_RALT;
            default -> -1;
        };
    }

    @Override
    public void removed() {
        if (this.suspended) {
            this.suspended = false;
            return;
        }

        boolean stuck = stopEngine();

        if (!stuck) {
            this.minecraft.getTextureManager().release(this.frameLocation);
            this.dynamicTexture = null;
            this.currentDoomFrame = null;
        }
        super.removed();
    }
}
