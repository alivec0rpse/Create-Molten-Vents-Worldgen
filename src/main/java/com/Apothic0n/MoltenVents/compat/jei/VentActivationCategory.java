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
    private static final int WIDTH  = 120;
    /** Height of the category background */
    private static final int HEIGHT = 62;

    // "Superheat" label row
    private static final int LABEL_Y    = 2;

    // Slot row — vertically centered in the remaining space below the label
    private static final int SLOT_Y     = 18;
    private static final int DORMANT_X  = 6;
    private static final int ACTIVE_X   = 84;

    // Arrow: horizontally centered in the gap between the two slots (24 to 84, centre = 54)
    private static final int ARROW_X    = 46;
    private static final int ARROW_Y    = 22;

    // Time label centred below the arrow
    private static final int TIME_Y     = 40;

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
        // Input: dormant vent — slot box top-left at (DORMANT_X, SLOT_Y)
        builder.addSlot(RecipeIngredientRole.INPUT, DORMANT_X, SLOT_Y)
               .addIngredients(VanillaTypes.ITEM_STACK,
                       java.util.List.of(recipe.getDormantStack()));

        // Output: active vent
        builder.addSlot(RecipeIngredientRole.OUTPUT, ACTIVE_X, SLOT_Y)
               .addIngredients(VanillaTypes.ITEM_STACK,
                       java.util.List.of(recipe.getActiveStack()));
    }

    @Override
    public void draw(VentActivationRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // ── "Superheat" header, centered ──────────────────────────────────────
        String superheatStr = Component.translatable("jei.molten_vents.superheat").getString();
        int superheatW = font.width(superheatStr);
        guiGraphics.drawString(font, superheatStr,
                (WIDTH - superheatW) / 2, LABEL_Y,
                0xFF_FF_CC_66,   // warm gold
                false);

        // ── Arrow ─────────────────────────────────────────────────────────────
        guiGraphics.drawString(font, "\u2192", ARROW_X, ARROW_Y, 0xFF_FF_FF_FF, false);

        // ── Time label, centred under the arrow ───────────────────────────────
        String timeLabel = recipe.getActivationSeconds() + "s";
        int timeW = font.width(timeLabel);
        // centre over the arrow gap (gap runs from right edge of left slot x=24 to left edge of right slot x=84, centre=54)
        guiGraphics.drawString(font, timeLabel,
                54 - timeW / 2, TIME_Y,
                0xFF_FF_AA_00,   // orange
                false);
    }
}
