package com.netherairtune.minedoom.client;

import com.netherairtune.minedoom.client.block.DoomBlock;
import com.netherairtune.minedoom.client.registry.ModBlocks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public class MineDoomClient implements ClientModInitializer {
    public static KeyMapping openDoomKey;

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as
        // rendering.

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
