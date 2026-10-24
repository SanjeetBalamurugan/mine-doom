package com.netherairtune.minedoom.client.gui;

import com.doom.DoomMaps;
import com.doom.WadHelper;
import com.doom.config.DoomConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public class DoomSettingsScreen extends Screen {
    private static final int COL_W = 180;
    private static final int GAP = 10;
    private static final int ROW_H = 24;

    private final DoomUIScreen parent;
    private final DoomConfig config = DoomConfig.get();
    private Button difficultyButton;
    private int titleY;
    private int noteY;

    public DoomSettingsScreen(DoomUIScreen parent) {
        super(Component.literal("MineDoom Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int leftX = this.width / 2 - COL_W - GAP / 2;
        int rightX = this.width / 2 + GAP / 2;
        int top = this.height / 2 - 80;
        this.titleY = top - 26;

        toggle(leftX, top, "Sound", () -> config.soundEnabled, v -> config.soundEnabled = v);
        this.addRenderableWidget(new IntSlider(leftX, top + ROW_H, COL_W, 20, 0, 100, config.soundVolume,
                v -> "Volume: " + v + "%", v -> config.soundVolume = v));
        this.addRenderableWidget(new IntSlider(leftX, top + ROW_H * 2, COL_W, 20, 30, 100, config.windowScale,
                v -> "Window Size: " + v + "%", v -> config.windowScale = v));
        toggle(leftX, top + ROW_H * 3, "Rounded Corners", () -> config.roundedCorners, v -> config.roundedCorners = v);
        toggle(leftX, top + ROW_H * 4, "Show FPS", () -> config.showFps, v -> config.showFps = v);

        Path gameDir = this.minecraft.gameDirectory.toPath();
        Path wad = WadHelper.getSelectedWad(gameDir, config.lastWad);
        List<String> maps = WadHelper.getMapLumps(wad);
        String wadName = wad == null ? "" : wad.getFileName().toString().toLowerCase(Locale.ROOT);

        this.difficultyButton = this.addRenderableWidget(Button.builder(difficultyText(), b -> {
            config.skill = config.skill % DoomConfig.SKILL_NAMES.length + 1;
            b.setMessage(difficultyText());
        }).bounds(rightX, top + ROW_H, COL_W, 20).build());

        int startIndex = maps.indexOf(config.startMap) + 1;
        if (maps.isEmpty()) {
            Button none = this.addRenderableWidget(Button.builder(Component.literal("Start: Main Menu (no maps found)"), b -> {})
                    .bounds(rightX, top, COL_W, 20).build());
            none.active = false;
            config.startMap = "";
            this.difficultyButton.active = false;
        } else {
            this.addRenderableWidget(new IntSlider(rightX, top, COL_W, 20, 0, maps.size(), startIndex,
                    i -> "Start: " + (i == 0 ? "Main Menu" : DoomMaps.label(maps.get(i - 1), wadName)),
                    i -> {
                        config.startMap = i == 0 ? "" : maps.get(i - 1);
                        this.difficultyButton.active = i > 0;
                    }));
            this.difficultyButton.active = startIndex > 0;
        }

        toggle(rightX, top + ROW_H * 2, "No Monsters", () -> config.noMonsters, v -> config.noMonsters = v);
        toggle(rightX, top + ROW_H * 3, "Respawn", () -> config.respawn, v -> config.respawn = v);
        toggle(rightX, top + ROW_H * 4, "Fast Monsters", () -> config.fastMonsters, v -> config.fastMonsters = v);

        this.addRenderableWidget(Button.builder(Component.literal("Reset to Defaults"), b -> {
            config.reset();
            this.rebuildWidgets();
        }).bounds(leftX, top + ROW_H * 5, COL_W, 20).build());

        Button changeWad = this.addRenderableWidget(Button.builder(Component.literal("Change WAD (restarts Doom)"), b -> {
            config.save();
            parent.requestWadChange();
            this.onClose();
        }).bounds(rightX, top + ROW_H * 5, COL_W, 20).build());
        changeWad.active = WadHelper.findWads(gameDir).size() > 1;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(leftX, top + ROW_H * 6, COL_W * 2 + GAP, 20).build());

        this.noteY = top + ROW_H * 7 + 2;
    }

    private Component difficultyText() {
        return Component.literal("Difficulty: " + DoomConfig.skillLabel(config.skill));
    }

    private static Component toggleText(String label, boolean value) {
        return Component.literal(label + ": " + (value ? "ON" : "OFF"));
    }

    private void toggle(int x, int y, String label, BooleanSupplier get, Consumer<Boolean> set) {
        this.addRenderableWidget(Button.builder(toggleText(label, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(toggleText(label, get.getAsBoolean()));
        }).bounds(x, y, COL_W, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        config.save();
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Component title = this.title.copy().withStyle(ChatFormatting.BOLD);
        graphics.text(this.font, title, (this.width - this.font.width(title)) / 2, titleY, 0xFFFFFFFF, true);

        Component note = Component.literal("Start map and game options apply the next time Doom starts.").withStyle(ChatFormatting.GRAY);
        graphics.text(this.font, note, (this.width - this.font.width(note)) / 2, noteY, 0xFFAAAAAA, false);
    }

    private static class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntFunction<String> text;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int w, int h, int min, int max, int value, IntFunction<String> text, IntConsumer onChange) {
            super(x, y, w, h, Component.empty(), (double) (value - min) / (max - min));
            this.min = min;
            this.max = max;
            this.text = text;
            this.onChange = onChange;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(text.apply(current())));
        }

        @Override
        protected void applyValue() {
            int v = current();
            this.value = (double) (v - min) / (max - min);
            onChange.accept(v);
        }
    }
}
