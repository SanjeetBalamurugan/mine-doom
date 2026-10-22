package com.netherairtune.minedoom.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class CrtToggleButton extends AbstractButton {
    private boolean isCrtEnabled = false;

    public CrtToggleButton(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("CRT"));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.isCrtEnabled = !this.isCrtEnabled;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.getX(), this.getY(), this.getX() + 40, this.getY() + this.getHeight(), 0xFF000000);
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + 39, this.getY() + this.getHeight() - 1, 0xFFFFFFFF);
        if (this.isCrtEnabled) {
            graphics.fill(this.getX() + 20, this.getY() + 1, this.getX() + 39, this.getY() + this.getHeight() - 1, 0xFF1C5771);
        } else {
            graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + 20, this.getY() + this.getHeight() - 1, 0xFF1C5771);
        }

        graphics.text(
            Minecraft.getInstance().font,
            this.getMessage().toString(),
            this.getX() + 45,
            this.getY() + (this.getHeight() - 8) / 2,
            0xFF005500, // Dark green
            false
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
