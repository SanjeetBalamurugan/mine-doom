package com.netherairtune.minedoom.client.gui;

import com.doom.DoomJNI;
import com.doom.WadHelper;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
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

import java.io.IOException;
import java.nio.file.Path;

public class DoomUIScreen extends Screen {
    private NativeImage currentDoomFrame;
    private DynamicTexture dynamicTexture;
    private Identifier frameLocation;
    private int doomWidth = 320;
    private int doomHeight = 200;
    private final int viewScale = 2;
    private Thread doomEngineThread;
    private SourceDataLine audioLine;
    private volatile boolean isRunning = true;
    private int lastMouseX;
    private boolean firstMouse = true;

    public DoomUIScreen() {
        super(Component.literal("DOOM UI"));
    }

    @Override
    protected void init() {
        super.init();

        Path mcDir = this.minecraft.gameDirectory.toPath();
        Path wadName;
        try {
            wadName = WadHelper.getWadFile(mcDir);
            if (wadName == null) {
                throw new IOException("WAD file could not be resolved by WadHelper.");
            }

            String iwadPath = mcDir.resolve(wadName).toAbsolutePath().getParent().toString();
            System.out.println("Passing IWAD Directory: " + iwadPath);

            if (!DoomJNI.isPlayerReady()) {
                DoomJNI.init(new String[]{
                    "-iwad", iwadPath,
                    "-skill", "3"
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (DoomJNI.isPlayerReady()) {
            this.doomWidth = DoomJNI.getScreenWidth();
            this.doomHeight = DoomJNI.getScreenHeight();
        }

        if (this.currentDoomFrame == null) {
            this.currentDoomFrame = new NativeImage(NativeImage.Format.RGBA, doomWidth, doomHeight, false);
        }

        if (this.dynamicTexture == null) {
            this.dynamicTexture = new DynamicTexture(null, this.currentDoomFrame);
            this.frameLocation = Identifier.fromNamespaceAndPath("minedoom", "doom_render_frame");
            this.minecraft.getTextureManager().register(this.frameLocation, this.dynamicTexture);
        }

        int scaledWidth = doomWidth * viewScale;
        int scaledHeight = doomHeight * viewScale;
        int startX = (this.width - scaledWidth) / 2;
        int startY = (this.height - scaledHeight) / 2;

        this.addRenderableWidget(new BedrockCloseButton(startX + scaledWidth, startY - 20, this::onClose));

        this.addRenderableWidget(new CrtToggleButton(startX + scaledWidth - 60, startY + scaledHeight + 15, 60, 20));

        this.minecraft.mouseHandler.grabMouse();
        this.firstMouse = true;

        startDoomEngine();
    }

    private void startDoomEngine() {
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
                if (DoomJNI.isPlayerReady()) {
                    DoomJNI.runTick();

                    short[] audioBuffer = DoomJNI.drainSound();
                    if (audioBuffer != null && audioBuffer.length > 0 && this.audioLine != null) {
                        byte[] byteBuffer = new byte[audioBuffer.length * 2];
                        for (int i = 0; i < audioBuffer.length; i++) {
                            byteBuffer[i * 2] = (byte) (audioBuffer[i] & 0xFF);
                            byteBuffer[i * 2 + 1] = (byte) ((audioBuffer[i] >> 8) & 0xFF);
                        }
                        this.audioLine.write(byteBuffer, 0, byteBuffer.length);
                    }
                }
                try { Thread.sleep(1000 / 35); } catch (InterruptedException ignored) {}
            }
        });
        this.doomEngineThread.setDaemon(true);
        this.doomEngineThread.start();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        int scaledWidth = doomWidth * viewScale;
        int scaledHeight = doomHeight * viewScale;
        int startX = (this.width - scaledWidth) / 2;
        int startY = (this.height - scaledHeight) / 2;

        if (DoomJNI.isPlayerReady()) {
            DoomJNI.renderFrame();
            byte[] pixels = DoomJNI.getScreenBuffer();
            byte[] palette = DoomJNI.getPalette();

            if (pixels != null && palette != null) {
                for (int y = 0; y < doomHeight; y++) {
                    for (int x = 0; x < doomWidth; x++) {
                        int index = pixels[y * doomWidth + x] & 0xFF;
                        int palIdx = index * 3;
                        int r = palette[palIdx] & 0xFF;
                        int g = palette[palIdx + 1] & 0xFF;
                        int b = palette[palIdx + 2] & 0xFF;
                        int abgr = (255 << 24) | (b << 16) | (g << 8) | r;
                        this.currentDoomFrame.setPixelABGR(x, y, abgr);
                    }
                }
                this.dynamicTexture.upload();
            }
        }

        if (this.frameLocation != null) {
            graphics.blit(this.frameLocation, startX, startY, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
        }

        int panelY = startY + scaledHeight;
        graphics.fill(startX, panelY, startX + scaledWidth, panelY + 50, 0xFFDDDDDD);

        graphics.pose().pushMatrix();
        graphics.pose().scale(2.5f, 2.5f);
        graphics.text(Minecraft.getInstance().font, "MINEDOOM", (int)((startX + 20) / 2.5f), (int)((panelY + 15) / 2.5f), 0xDD0000, false);
        graphics.pose().popMatrix();

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int doomKey = mapKey(event.key());
        if (doomKey != -1) {
            DoomJNI.keyDown(doomKey);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        int doomKey = mapKey(event.key());
        if (doomKey != -1) {
            DoomJNI.keyUp(doomKey);
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
        if (event.button() == 0) DoomJNI.mouseButton(0, true);
        if (event.button() == 1) DoomJNI.mouseButton(1, true);
        return super.mouseClicked(event, doubleClicked);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) DoomJNI.mouseButton(0, false);
        if (event.button() == 1) DoomJNI.mouseButton(1, false);
        return super.mouseReleased(event);
    }

    @Override
    public void mouseMoved(double x, double y) {
        if (firstMouse) {
            lastMouseX = (int) x;
            firstMouse = false;
            return;
        }
        int dx = (int) (x - lastMouseX);
        lastMouseX = (int) x;
        DoomJNI.mouseMove(dx, 0);
        super.mouseMoved(x, y);
    }

    private int mapKey(int k) {
        return switch (k) {
            case 256 -> 27;
            case 87, 265 -> DoomJNI.KEY_UPARROW;
            case 83, 264 -> DoomJNI.KEY_DOWNARROW;
            case 65, 263 -> DoomJNI.KEY_LEFTARROW;
            case 68, 262 -> DoomJNI.KEY_RIGHTARROW;
            case 340, 344 -> DoomJNI.KEY_RSHIFT;
            case 341, 345 -> DoomJNI.KEY_RCTRL;
            case 342, 346 -> DoomJNI.KEY_RALT;
            case 32 -> ' ';
            case 257 -> DoomJNI.KEY_ENTER;
            case 258 -> DoomJNI.KEY_TAB;
            case 259 -> DoomJNI.KEY_BACKSPACE;
            case 49 -> '1';
            case 50 -> '2';
            case 51 -> '3';
            case 52 -> '4';
            case 53 -> '5';
            case 54 -> '6';
            case 55 -> '7';
            case 69 -> 'e';
            default -> -1;
        };
    }

    @Override
    public void removed() {
        this.isRunning = false;
        this.minecraft.mouseHandler.releaseMouse();

        if (this.audioLine != null) {
            this.audioLine.stop();
            this.audioLine.close();
        }
        if (this.dynamicTexture != null) {
            this.dynamicTexture.close();
        }
        if (this.currentDoomFrame != null) {
            this.currentDoomFrame.close();
        }
        super.removed();
    }
}
