package com.netherairtune.minedoom.client.block;

import com.netherairtune.minedoom.client.gui.DoomUIScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DoomBlock extends Block {

    public DoomBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui.screen() == null) {
                mc.setScreenAndShow(new DoomUIScreen());
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }
}
