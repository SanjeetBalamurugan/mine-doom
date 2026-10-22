package com.netherairtune.minedoom.client;

import javax.swing.text.JTextComponent.KeyBinding;

import com.mojang.blaze3d.platform.InputConstants;
import com.netherairtune.minedoom.client.block.DoomBlock;
import com.netherairtune.minedoom.client.gui.DoomUIScreen;
import com.netherairtune.minedoom.client.registry.ModBlocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

public class MineDoomClient implements ClientModInitializer {
    public static KeyMapping openDoomKey;
    private static final KeyMapping.Category DOOM_CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("minedoom", "main"));

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as
        // rendering.

        openDoomKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.minedoom.open",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_M, // Press 'M' to open DOOM
            DOOM_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openDoomKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.setScreenAndShow(new DoomUIScreen());
                }
            }
        });

        ModBlocks.init(properties -> new DoomBlock(properties));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                if (client.player != null) {
                    Component message = Component.literal("Thanks for installing ")
                            .append(Component.literal("DoomMC")
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0xFF0000))))
                            .append(Component.literal("! Have fun!")
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0x00FF00))));
                    client.player.sendSystemMessage(message);
                }
            });
        });
    }
}
