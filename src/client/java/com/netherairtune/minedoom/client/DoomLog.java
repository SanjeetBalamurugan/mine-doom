package com.netherairtune.minedoom.client;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public final class DoomLog {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DoomLog() {
    }

    public static void error(Component message) {
        Minecraft mc = Minecraft.getInstance();
        String playerName = playerName(mc);
        LOGGER.error("[MineDoom] {}: {}", playerName, message.getString());
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.translatable("minedoom.log.error", playerName, message)
                                .withStyle(ChatFormatting.RED));
            }
        });
    }

    public static void error(String translationKey, Object... args) {
        error(Component.translatable(translationKey, args));
    }

    public static void error(Throwable throwable) {
        String detail = throwable.getMessage() != null
                ? throwable.getMessage()
                : throwable.getClass().getSimpleName();
        LOGGER.error("[MineDoom] Exception", throwable);
        error(Component.translatable("minedoom.screen.error.body", detail));
    }

    private static String playerName(Minecraft mc) {
        if (mc.player != null) {
            return mc.player.getName().getString();
        }
        return mc.getUser().getName();
    }
}
