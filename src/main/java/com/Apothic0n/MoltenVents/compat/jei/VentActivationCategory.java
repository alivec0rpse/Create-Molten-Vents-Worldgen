package com.Apothic0n.MoltenVents.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * JEI recipe category displaying dormant vent + superheated blaze burner → active vent.
 */
public class VentActivationCategory implements IRecipeCategory<VentActivationRecipe> {

    public static final RecipeType<VentActivationRecipe> RECIPE_TYPE = RecipeType.create(
            "molten_vents", "vent_activation", VentActivationRecipe.class);

    /** Width of the category background */
    private static final int WIDTH  = 116;
    /** Height of the category background */
    private static final int HEIGHT = 36;

    // Slot positions (top-left of each 16×16 slot icon)
    private static final int DORMANT_X  = 1;
    private static final int DORMANT_Y  = 10;
    private static final int ACTIVE_X   = 81;
    private static final int ACTIVE_Y   = 10;

    // Arrow area
    private static final int ARROW_X    = 36;
    private static final int ARROW_Y    = 10;

    private final IDrawable background;
    private final IDrawable icon;

    public VentActivationCategory(IGuiHelper guiHelper) {
        // Blank background sized to fit two slots + arrow
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);

        // Use blaze burner item as the category icon (falls back gracefully if not present)
        var blazeBurnerItem = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.tryParse("create:blaze_burner"))
                .orElse(Items.BLAZE_POWDER);
        this.icon = guiHelper.createDrawableItemStack(new net.minecraft.world.item.ItemStack(blazeBurnerItem));
    }

    // ------------------------------------------------------------------ IRecipeCategory

    @Override
    public RecipeType<VentActivationRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.molten_vents.vent_activation");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, VentActivationRecipe recipe, IFocusGroup focuses) {
        // Input: dormant vent
        builder.addSlot(RecipeIngredientRole.INPUT, DORMANT_X + 1, DORMANT_Y + 1)
               .addIngredients(VanillaTypes.ITEM_STACK,
                       java.util.List.of(recipe.getDormantStack()));

        // Output: active vent
        builder.addSlot(RecipeIngredientRole.OUTPUT, ACTIVE_X + 1, ACTIVE_Y + 1)
               .addIngredients(VanillaTypes.ITEM_STACK,
                       java.util.List.of(recipe.getActiveStack()));
    }

    @Override
    public void draw(VentActivationRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // Draw arrow between the two slots
        guiGraphics.drawString(font, "→", ARROW_X + 3, ARROW_Y + 4, 0xFF_FF_FF_FF, false);

        // Draw activation time label above arrow
        String timeLabel = recipe.getActivationSeconds() + "s";
        guiGraphics.drawString(font, timeLabel, ARROW_X + 2, ARROW_Y - 9, 0xFF_FF_AA_00, false);

        // Draw "Superheat" label at top
        guiGraphics.drawString(font,
                Component.translatable("jei.molten_vents.superheat").getString(),
                DORMANT_X, 0,
                0xFF_CC_CC_CC,
                false
        );
    }
}
