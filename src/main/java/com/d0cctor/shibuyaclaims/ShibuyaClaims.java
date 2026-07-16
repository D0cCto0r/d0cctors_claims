package com.d0cctor.shibuyaclaims;

import com.d0cctor.shibuyaclaims.commands.ClaimCommands;
import com.d0cctor.shibuyaclaims.events.ClaimEvents;
import com.d0cctor.shibuyaclaims.registry.ModBlocks;
import com.d0cctor.shibuyaclaims.registry.ModItems;
import com.d0cctor.shibuyaclaims.registry.ModEntities;
import com.d0cctor.shibuyaclaims.client.ClaimClientEvents;
import com.d0cctor.shibuyaclaims.network.ClaimNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

@Mod(ShibuyaClaims.MOD_ID)
public final class ShibuyaClaims {
    public static final String MOD_ID = "d0cctors_claims";

    public ShibuyaClaims(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(ClaimNetwork::register);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClaimClientEvents::registerRenderers);
            modEventBus.addListener(ClaimClientEvents::registerLayerDefinitions);
        }

        NeoForge.EVENT_BUS.register(new ClaimEvents());
        NeoForge.EVENT_BUS.addListener(ClaimCommands::onRegisterCommands);
    }
}
