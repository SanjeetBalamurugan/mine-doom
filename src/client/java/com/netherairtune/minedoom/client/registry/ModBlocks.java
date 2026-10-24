package com.netherairtune.minedoom.client.registry;

import com.netherairtune.minedoom.MineDoom;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.function.Function;

public class ModBlocks {

    public static Block DOOM_BLOCK;
    public static CreativeModeTab MINEDOOM_TAB;

    public static void init(Function<BlockBehaviour.Properties, Block> blockFactory) {
        DOOM_BLOCK = register("doom_block", blockFactory, BlockBehaviour.Properties.of().strength(4.0f));

        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(MineDoom.MOD_ID, "minedoom"));

        MINEDOOM_TAB = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                tabKey,
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemGroup.minedoom"))
                        .icon(() -> new ItemStack(DOOM_BLOCK))
                        .displayItems((parameters, output) -> output.accept(DOOM_BLOCK))
                        .build());
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MineDoom.MOD_ID, name));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MineDoom.MOD_ID, name));

        Block block = blockFactory.apply(properties.setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return block;
    }
}
