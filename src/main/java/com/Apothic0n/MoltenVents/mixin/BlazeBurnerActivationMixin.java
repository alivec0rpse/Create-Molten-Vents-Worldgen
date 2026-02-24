package com.Apothic0n.MoltenVents.mixin;

import com.Apothic0n.MoltenVents.config.MoltenVentsActivationConfig;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Injects into {@link BlazeBurnerBlockEntity#tick()} to detect when a blaze burner is
 * superheated while sitting on top of a dormant vent block.
 *
 * After {@link MoltenVentsActivationConfig#activationTicks} ticks of continuous
 * superheating the dormant vent is replaced by its configured active counterpart.
 *
 * The conversion counter is persisted in the block entity's NBT so it survives
 * chunk unloads and world saves.
 */
@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class BlazeBurnerActivationMixin {

    // ---- shadows ---------------------------------------------------------------

    @Shadow
    public abstract BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock();

    /** Shadowed from BlockEntity */
    @Shadow(remap = false)
    protected Level level;

    /** Shadowed from BlockEntity */
    @Shadow(remap = false)
    public abstract BlockPos getBlockPos();

    /** Shadowed from BlockEntity */
    @Shadow(remap = false)
    public abstract boolean hasLevel();

    /** Shadowed from BlockEntity */
    @Shadow(remap = false)
    public abstract void setChanged();

    /** Shadowed from SmartBlockEntity - returns true when used for ponder rendering */
    @Shadow
    public abstract boolean isVirtual();

    // ---- unique state ----------------------------------------------------------

    /**
     * Remaining ticks until conversion, or -1 when not actively converting.
     */
    @Unique
    private int moltenVents$conversionTimer = -1;

    // ---- NBT persistence -------------------------------------------------------

    @Inject(method = "write", at = @At("HEAD"))
    private void moltenVents$write(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        tag.putInt("moltenVentsConversionTimer", moltenVents$conversionTimer);
    }

    @Inject(method = "read", at = @At("HEAD"))
    private void moltenVents$read(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (tag.contains("moltenVentsConversionTimer")) {
            moltenVents$conversionTimer = tag.getInt("moltenVentsConversionTimer");
        } else {
            moltenVents$conversionTimer = -1;
        }
    }

    // ---- tick ------------------------------------------------------------------

    @Inject(method = "tick", at = @At("HEAD"))
    private void moltenVents$tick(CallbackInfo ci) {
        // Only run server-side
        if (!this.hasLevel() || this.isVirtual() || this.level.isClientSide()) return;

        BlockPos belowPos = this.getBlockPos().below();
        Block belowBlock = this.level.getBlockState(belowPos).getBlock();

        // Resolve registry key
        ResourceLocation belowId = BuiltInRegistries.BLOCK.getKey(belowBlock);
        if (belowId == null) {
            moltenVents$conversionTimer = -1;
            return;
        }

        String belowIdStr = belowId.toString();
        String activeIdStr = MoltenVentsActivationConfig.INSTANCE.getActiveId(belowIdStr);

        if (activeIdStr != null && this.getHeatLevelFromBlock().isAtLeast(BlazeBurnerBlock.HeatLevel.SEETHING)) {
            // Blaze burner is superheated on top of a registered dormant vent
            int maxTicks = MoltenVentsActivationConfig.INSTANCE.activationTicks;

            if (moltenVents$conversionTimer < 0) {
                // Start counting down
                moltenVents$conversionTimer = maxTicks;
                this.setChanged();
            } else if (moltenVents$conversionTimer == 0) {
                // Conversion complete — replace the block
                ResourceLocation activeId = ResourceLocation.tryParse(activeIdStr);
                if (activeId != null) {
                    Optional<Block> activeBlockOpt = BuiltInRegistries.BLOCK.getOptional(activeId);
                    activeBlockOpt.ifPresent(activeBlock -> {
                        this.level.setBlock(belowPos, activeBlock.defaultBlockState(), Block.UPDATE_ALL);
                    });
                }
                moltenVents$conversionTimer = -1;
                this.setChanged();
            } else {
                moltenVents$conversionTimer--;
                // Mark dirty every 20 ticks so NBT stays reasonably in sync
                if (moltenVents$conversionTimer % 20 == 0) {
                    this.setChanged();
                }
            }
        } else {
            // Not superheating (or no longer on a dormant vent) — reset
            if (moltenVents$conversionTimer >= 0) {
                moltenVents$conversionTimer = -1;
                this.setChanged();
            }
        }
    }
}
