package com.netherairtune.minedoom.client.gui;

import com.doom.DoomJNI;
import com.doom.WadHelper;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DoomUIScreen extends Screen {
    private static boolean engineInitialized = false;

    private NativeImage currentDoomFrame;
    private DynamicTexture dynamicTexture;
    private Identifier frameLocation;
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

    private BedrockCloseButton closeButton;
    private CrtToggleButton crtButton;

    private Thread doomEngineThread;
    private SourceDataLine audioLine;
    private volatile boolean isRunning = true;
    private volatile boolean frameReady = false;
    private final Object renderLock = new Object();
    private final ConcurrentLinkedQueue<Runnable> inputQueue = new ConcurrentLinkedQueue<>();

    public DoomUIScreen() {
        super(Component.literal("DOOM UI"));
    }

    @Override
    protected void init() {
        super.init();

        try {
            if (!engineInitialized) {
                Path mcDir = this.minecraft.gameDirectory.toPath();
                Path wadName = WadHelper.getWadFile(mcDir);
                if (wadName == null) {
                    System.err.println("[MineDoom] WAD file could not be resolved.");
                    return;
                }

                String iwadPath = mcDir.resolve(wadName).toAbsolutePath().getParent().toString();
                System.out.println("[MineDoom] Initializing DOOM engine with IWAD path: " + iwadPath);

                DoomJNI.init(new String[]{
                    "-iwad", iwadPath
                });
                engineInitialized = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (engineInitialized) {
            this.doomWidth = DoomJNI.getScreenWidth();
            this.doomHeight = DoomJNI.getScreenHeight();
        }

        int maxW = this.width - 40;
        int maxH = this.height - 40;
        float scale = Math.min((float) maxW / doomWidth, (float) maxH / (doomHeight + doomWidth * FOOTER_RATIO));
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
        this.crtButton = this.addRenderableWidget(new CrtToggleButton(0, 0, 75, 20));
        layoutButtons();

        startDoomEngine();
    }

    private void layoutButtons() {
        int footerY = windowY + windowHeight;
        int crtH = Math.max(12, (int) (footerHeight * 0.4f));
        int crtW = (int) (windowWidth * 0.2f);
        crtButton.setSize(crtW, crtH);
        crtButton.setPosition(windowX + (int) (windowWidth * 0.68f), footerY + (footerHeight - crtH) / 2);
        closeButton.setPosition(windowX + windowWidth - 24, footerY + (footerHeight - 20) / 2);
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

    private void startDoomEngine() {
        if (this.doomEngineThread != null || !engineInitialized) return;

        try {
            AudioFormat format = new AudioFormat(11025f, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            this.audioLine = (SourceDataLine) AudioSystem.getLine(info);
            this.audioLine.open(format);
            this.audioLine.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.doomEngineThread = new Thread(() -> {
            while (this.isRunning) {
                Runnable task;
                while ((task = inputQueue.poll()) != null) task.run();
                synchronized (renderLock) {
                    DoomJNI.runTick();
                    DoomJNI.renderFrame();

                    byte[] pixels = DoomJNI.getScreenBuffer();
                    byte[] palette = DoomJNI.getPalette();

                    if (pixels != null && palette != null && this.currentDoomFrame != null) {
                        for (int y = 0; y < doomHeight; y++) {
                            for (int x = 0; x < doomWidth; x++) {
                                if (isOutsideRoundedTop(x, y)) {
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
                        frameReady = true;
                    }
                }

                short[] audioBuffer = DoomJNI.drainSound();
                if (audioBuffer != null && audioBuffer.length > 0 && this.audioLine != null) {
                    byte[] byteBuffer = new byte[audioBuffer.length * 2];
                    for (int i = 0; i < audioBuffer.length; i++) {
                        byteBuffer[i * 2] = (byte) (audioBuffer[i] & 0xFF);
                        byteBuffer[i * 2 + 1] = (byte) ((audioBuffer[i] >> 8) & 0xFF);
                    }
                    this.audioLine.write(byteBuffer, 0, byteBuffer.length);
                }
                try { Thread.sleep(1000 / 35); } catch (InterruptedException ignored) {}
            }
        });
        this.doomEngineThread.setDaemon(true);
        this.doomEngineThread.start();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int footerY = windowY + windowHeight;

        if (!frameReady) {
            graphics.fill(windowX, windowY, windowX + windowWidth, footerY, 0xFF000000);
            graphics.text(this.font, "Loading DOOM...", windowX + windowWidth / 2 - 40, windowY + windowHeight / 2, 0xFFFFFFFF, false);
        } else {
            synchronized (renderLock) {
                if (this.dynamicTexture != null) {
                    this.dynamicTexture.upload();
                }
            }

            if (this.frameLocation != null) {
                graphics.blit(this.frameLocation, windowX, windowY, windowX + windowWidth, footerY, 0f, 1f, 0f, 1f);
            }
        }

        graphics.fill(windowX, footerY, windowX + windowWidth, footerY + footerHeight, FOOTER_BORDER_COLOR);
        graphics.fill(windowX + FOOTER_BORDER, footerY + FOOTER_BORDER,
                windowX + windowWidth - FOOTER_BORDER, footerY + footerHeight - FOOTER_BORDER,
                FOOTER_BG_COLOR);

        float ts = footerHeight / 15f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(windowX + windowWidth * 0.04f, footerY + (footerHeight - 8 * ts) / 2f);
        graphics.pose().scale(ts, ts);
        graphics.text(this.font, Component.literal("MINEDOOM").withStyle(ChatFormatting.BOLD), 0, 0, TITLE_COLOR, false);
        graphics.pose().popMatrix();

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int doomKey = mapKey(event.key());
        if (doomKey != -1) {
            inputQueue.add(() -> DoomJNI.keyDown(doomKey));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        int doomKey = mapKey(event.key());
        if (doomKey != -1) {
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
        return switch (k) {
            case 41 -> 27;
            case 26, 82 -> DoomJNI.KEY_UPARROW;
            case 22, 81 -> DoomJNI.KEY_DOWNARROW;
            case 4, 80 -> DoomJNI.KEY_LEFTARROW;
            case 7, 79 -> DoomJNI.KEY_RIGHTARROW;
            case 225, 229 -> DoomJNI.KEY_RSHIFT;
            case 224, 228 -> DoomJNI.KEY_RCTRL;
            case 226, 230 -> DoomJNI.KEY_RALT;
            case 44 -> ' ';
            case 40 -> DoomJNI.KEY_ENTER;
            case 43 -> DoomJNI.KEY_TAB;
            case 42 -> DoomJNI.KEY_BACKSPACE;
            case 30 -> '1';
            case 31 -> '2';
            case 32 -> '3';
            case 33 -> '4';
            case 34 -> '5';
            case 35 -> '6';
            case 36 -> '7';
            case 8 -> 'e';
            case 28 -> 'y';
            case 17 -> 'n';
            case 54 -> ',';
            case 55 -> '.';
            case 45 -> '-';
            case 46 -> '=';
            default -> -1;
        };
    }

    @Override
    public void removed() {
        this.isRunning = false;
        if (this.audioLine != null) {
            this.audioLine.stop();
            this.audioLine.close();
        }
        try { if (this.doomEngineThread != null) this.doomEngineThread.join(500); } catch (InterruptedException ignored) {}
        if (this.dynamicTexture != null) {
            this.dynamicTexture.close();
        }
        if (this.currentDoomFrame != null) {
            this.currentDoomFrame.close();
        }
        super.removed();
    }
}
