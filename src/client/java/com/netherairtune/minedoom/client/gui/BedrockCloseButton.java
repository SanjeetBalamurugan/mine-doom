package com.netherairtune.minedoom.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class BedrockCloseButton extends AbstractButton {
    private final Runnable onClose;

    public BedrockCloseButton(int x, int y, Runnable onClose) {
        super(x, y, 20, 20, Component.literal("X"));
        this.onClose = onClose;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onClose.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

        int bgColor = isHovered ? 0xFFF1707A : 0xFFE81123;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

        graphics.text(
            Minecraft.getInstance().font,
            this.getMessage().getString(),
            this.getX() + 7,
            this.getY() + 6,
            0xFFFFFFFF,
            false
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
