package com.d0cctor.shibuyaclaims.registry;

import com.d0cctor.shibuyaclaims.ShibuyaClaims;
import com.d0cctor.shibuyaclaims.claim.ClaimCoreBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ShibuyaClaims.MOD_ID);

    public static final DeferredHolder<Block, Block> CLAIM_CORE = BLOCKS.register("claim_core", () -> new ClaimCoreBlock(
            BlockBehaviour.Properties.of()
                    .strength(4.0F, 1200.0F)
                    .lightLevel(state -> 10)
                    .noOcclusion()
                    .sound(SoundType.AMETHYST)
    ));

    private ModBlocks() {}
}
