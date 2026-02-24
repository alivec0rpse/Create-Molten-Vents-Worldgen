package com.Apothic0n.MoltenVents.mixin;

import com.Apothic0n.MoltenVents.config.MoltenVentsActivationConfig;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Injects into {@link BlazeBurnerBlockEntity} to detect when a blaze burner is
 * superheated while sitting on top of a dormant vent block.
 *
 * After {@link MoltenVentsActivationConfig#activationTicks} ticks of continuous
 * superheating, the dormant vent is replaced by its configured active counterpart.
 *
 * BlockEntity-inherited methods (getBlockPos, getLevel, setChanged, hasLevel) are
 * accessed via a (BlockEntity) cast to avoid needing a refMap for obfuscated names.
 * Only Create-owned methods use @Shadow (remap=false).
 */
@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class BlazeBurnerActivationMixin {

    // ---- shadows (Create methods — remap=false is correct) ---------------------

    @Shadow
    public abstract BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock();

    // isVirtual() is on SmartBlockEntity (parent) — not shadowed to avoid Ponder compile dep.
    // Ponder virtual worlds are client-side, so the isClientSide() guard in the tick is sufficient.

    // ---- unique state ----------------------------------------------------------

    @Unique
    private int moltenVents$conversionTimer = -1;

    // ---- helper ----------------------------------------------------------------

    /** Cast to BlockEntity so we can call vanilla-inherited methods without a refMap. */
    @Unique
    private BlockEntity moltenVents$self() {
        return (BlockEntity) (Object) this;
    }



    // ---- NBT persistence -------------------------------------------------------

    @Inject(method = "write", at = @At("HEAD"))
    private void moltenVents$write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        tag.putInt("moltenVentsConversionTimer", moltenVents$conversionTimer);
    }

    @Inject(method = "read", at = @At("HEAD"))
    private void moltenVents$read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (tag.contains("moltenVentsConversionTimer")) {
            moltenVents$conversionTimer = tag.getInt("moltenVentsConversionTimer");
        } else {
            moltenVents$conversionTimer = -1;
        }
    }

    // ---- tick ------------------------------------------------------------------

    @Inject(method = "tick", at = @At("HEAD"))
    private void moltenVents$tick(CallbackInfo ci) {
        BlockEntity be = moltenVents$self();

        // Only run server-side (Ponder virtual worlds are also client-side, so this covers both)
        if (!be.hasLevel() || be.getLevel().isClientSide()) return;

        BlockPos belowPos = be.getBlockPos().below();
        Block belowBlock = be.getLevel().getBlockState(belowPos).getBlock();

        ResourceLocation belowId = BuiltInRegistries.BLOCK.getKey(belowBlock);
        if (belowId == null) {
            moltenVents$conversionTimer = -1;
            return;
        }

        String activeIdStr = MoltenVentsActivationConfig.INSTANCE.getActiveId(belowId.toString());

        if (activeIdStr != null && this.getHeatLevelFromBlock().isAtLeast(BlazeBurnerBlock.HeatLevel.SEETHING)) {
            int maxTicks = MoltenVentsActivationConfig.INSTANCE.activationTicks;

            if (moltenVents$conversionTimer < 0) {
                moltenVents$conversionTimer = maxTicks;
                be.setChanged();
            } else if (moltenVents$conversionTimer == 0) {
                ResourceLocation activeId = ResourceLocation.tryParse(activeIdStr);
                if (activeId != null) {
                    Optional<Block> activeBlockOpt = BuiltInRegistries.BLOCK.getOptional(activeId);
                    activeBlockOpt.ifPresent(activeBlock ->
                            be.getLevel().setBlock(belowPos, activeBlock.defaultBlockState(), Block.UPDATE_ALL));
                }
                moltenVents$conversionTimer = -1;
                be.setChanged();
            } else {
                moltenVents$conversionTimer--;
                if (moltenVents$conversionTimer % 20 == 0) {
                    be.setChanged();
                }
            }
        } else {
            if (moltenVents$conversionTimer >= 0) {
                moltenVents$conversionTimer = -1;
                be.setChanged();
            }
        }
    }
}
