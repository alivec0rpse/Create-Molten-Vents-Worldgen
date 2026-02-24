package com.Apothic0n.MoltenVents.compat.jei;

import net.minecraft.world.item.ItemStack;

/**
 * Represents one dormant → active vent activation pair for JEI display.
 */
public class VentActivationRecipe {

    private final ItemStack dormantStack;
    private final ItemStack activeStack;
    private final int activationTicks;

    public VentActivationRecipe(ItemStack dormantStack, ItemStack activeStack, int activationTicks) {
        this.dormantStack = dormantStack;
        this.activeStack = activeStack;
        this.activationTicks = activationTicks;
    }

    public ItemStack getDormantStack() {
        return dormantStack;
    }

    public ItemStack getActiveStack() {
        return activeStack;
    }

    /** Activation time in ticks */
    public int getActivationTicks() {
        return activationTicks;
    }

    /** Activation time in seconds (rounded) */
    public int getActivationSeconds() {
        return activationTicks / 20;
    }
}
