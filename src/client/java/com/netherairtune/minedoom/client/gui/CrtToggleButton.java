package com.netherairtune.minedoom.client.gui;

import net.minecraft.ChatFormatting;
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

    public boolean isCrtEnabled() {
        return this.isCrtEnabled;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.isCrtEnabled = !this.isCrtEnabled;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int h = this.getHeight();
        int x = this.getX();
        int y = this.getY();
        int tw = (int) (h * 2.2f);
        int b = 2;
        int mid = x + tw / 2;

        graphics.fill(x, y, x + tw, y + h, 0xFF000000);
        graphics.fill(x + b, y + b, x + tw - b, y + h - b, 0xFFE8E8E8);
        if (this.isCrtEnabled) {
            graphics.fill(mid, y + b, x + tw - b, y + h - b, 0xFF1C5771);
        } else {
            graphics.fill(x + b, y + b, mid, y + h - b, 0xFF1C5771);
        }

        float ts = h / 14f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + tw + h * 0.35f, y + (h - 8 * ts) / 2f);
        graphics.pose().scale(ts, ts);
        graphics.text(
            Minecraft.getInstance().font,
            this.getMessage().copy().withStyle(ChatFormatting.BOLD),
            0,
            0,
            0xFF3A7D1E,
            false
        );
        graphics.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
