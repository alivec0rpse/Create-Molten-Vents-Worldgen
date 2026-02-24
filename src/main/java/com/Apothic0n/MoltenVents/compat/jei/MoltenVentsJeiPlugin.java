package com.Apothic0n.MoltenVents.compat.jei;

import com.Apothic0n.MoltenVents.config.MoltenVentsActivationConfig;
import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JEI plugin for Molten Vents.
 *
 * Automatically loaded by JEI when the mod is present — no extra wiring needed.
 * If JEI is absent, this class is simply never instantiated.
 */
@JeiPlugin
public class MoltenVentsJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("molten_vents", "jei_plugin");
    }

    // ------------------------------------------------------------------ categories

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new VentActivationCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    // ------------------------------------------------------------------ recipes

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<VentActivationRecipe> recipes = buildRecipes();
        if (!recipes.isEmpty()) {
            registration.addRecipes(VentActivationCategory.RECIPE_TYPE, recipes);
        }
    }

    // ------------------------------------------------------------------ catalysts

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Blaze burner acts as the catalyst (shown in the JEI item-use lookup)
        ResourceLocation blazeBurnerId = ResourceLocation.tryParse("create:blaze_burner");
        if (blazeBurnerId != null) {
            Optional<Block> blazeBurner = BuiltInRegistries.BLOCK.getOptional(blazeBurnerId);
            blazeBurner.ifPresent(b -> registration.addRecipeCatalyst(
                    new ItemStack(b), VentActivationCategory.RECIPE_TYPE));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static List<VentActivationRecipe> buildRecipes() {
        List<VentActivationRecipe> list = new ArrayList<>();
        int ticks = MoltenVentsActivationConfig.INSTANCE.activationTicks;

        for (MoltenVentsActivationConfig.VentPair pair : MoltenVentsActivationConfig.INSTANCE.ventPairs) {
            if (pair.dormant == null || pair.active == null) continue;

            ResourceLocation dormantId = ResourceLocation.tryParse(pair.dormant);
            ResourceLocation activeId  = ResourceLocation.tryParse(pair.active);
            if (dormantId == null || activeId == null) continue;

            Optional<Block> dormantBlock = BuiltInRegistries.BLOCK.getOptional(dormantId);
            Optional<Block> activeBlock  = BuiltInRegistries.BLOCK.getOptional(activeId);

            if (dormantBlock.isEmpty() || activeBlock.isEmpty()) {
                LOGGER.debug("[MoltenVents JEI] Skipping pair {}->{}: block(s) not found in registry.",
                        pair.dormant, pair.active);
                continue;
            }

            list.add(new VentActivationRecipe(
                    new ItemStack(dormantBlock.get()),
                    new ItemStack(activeBlock.get()),
                    ticks
            ));
        }

        return list;
    }
}
