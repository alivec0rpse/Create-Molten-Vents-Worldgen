package com.Apothic0n.MoltenVents;

import com.Apothic0n.MoltenVents.api.biome.features.MoltenVentsFeatures;
import com.Apothic0n.MoltenVents.config.MoltenVentsActivationConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

// The value here should match an entry in the META-INF/mods.toml file.
@Mod("molten_vents")
public class MoltenVents
{
    public static final String MODID = "molten_vents";

    public MoltenVents(IEventBus eventBus, ModContainer container) throws Exception {
        MoltenVentsFeatures.register(eventBus);

        // Load dormant→active vent activation config once everything has registered
        eventBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        MoltenVentsActivationConfig.load();
    }
}
