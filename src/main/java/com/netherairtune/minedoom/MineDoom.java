package com.netherairtune.minedoom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public class MineDoom implements ModInitializer {
	public static final String MOD_ID = "minedoom";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("DoomMC Initializing");

        FabricLoader client = FabricLoader.getInstance();
        Path gameDir = client.getGameDir();
        Path mineDoomDir = gameDir.resolve("config/" + MOD_ID);

        try {
            Files.createDirectories(mineDoomDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create config directory: " + mineDoomDir, e);
        }
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
