package com.xeno.goop.fluids;

import net.minecraft.item.Item;
import net.minecraftforge.fluids.FluidAttributes;

import java.util.function.Supplier;

public class ChromaticGoop extends GoopBase {
    public ChromaticGoop(Supplier<? extends Item> bucket, FluidAttributes.Builder builder) {
        super(bucket, builder);
    }
}