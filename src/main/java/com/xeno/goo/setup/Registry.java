package com.xeno.goo.setup;

import com.xeno.goo.GooMod;
import com.xeno.goo.blocks.*;
import com.xeno.goo.fluids.*;
import com.xeno.goo.items.*;
import com.xeno.goo.tiles.GooBulbTile;
import com.xeno.goo.tiles.GooifierTile;
import com.xeno.goo.tiles.SolidifierTile;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class Registry {


    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GooMod.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, GooMod.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GooMod.MOD_ID);
    // private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, GooMod.MOD_ID);
    private static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, GooMod.MOD_ID);
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, GooMod.MOD_ID);
    // private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, GooMod.MOD_ID);

    public static void init () {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        FLUIDS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TILES.register(FMLJavaModLoadingContext.get().getModEventBus());
        CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        // ENCHANTMENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        // ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    public static final RegistryObject<Gasket> GASKET = ITEMS.register("gasket", Gasket::new);
//    public static final RegistryObject<Gauntlet> GAUNTLET = ITEMS.register("gauntlet", Gauntlet::new);
//    public static final RegistryObject<Crucible> CRUCIBLE = ITEMS.register("crucible", Crucible::new);

    // Goo Bulbs registration
    public static final RegistryObject<GooBulb> GOO_BULB = BLOCKS.register("goo_bulb", GooBulb::new);
    public static final RegistryObject<Item> GOO_BULB_ITEM = ITEMS.register("goo_bulb", () -> new BlockItem(GOO_BULB.get(), new Item.Properties().group(GooMod.ITEM_GROUP).maxStackSize(1)));
    public static final RegistryObject<TileEntityType<GooBulbTile>> GOO_BULB_TILE = TILES.register("goo_bulb", () -> TileEntityType.Builder.create(GooBulbTile::new, GOO_BULB.get()).build(null));

    // Gooifier registration
    public static final RegistryObject<Gooifier> GOOIFIER = BLOCKS.register("gooifier", Gooifier::new);
    public static final RegistryObject<Item> GOOIFIER_ITEM = ITEMS.register("gooifier", () -> new BlockItem(GOOIFIER.get(), new Item.Properties().group(GooMod.ITEM_GROUP).maxStackSize(1)));
    public static final RegistryObject<TileEntityType<GooifierTile>> GOOIFIER_TILE = TILES.register("gooifier", () -> TileEntityType.Builder.create(GooifierTile::new, GOOIFIER.get()).build(null));

    // Solidifier registration
    public static final RegistryObject<Solidifier> SOLIDIFIER = BLOCKS.register("solidifier", Solidifier::new);
    public static final RegistryObject<Item> SOLIDIFIER_ITEM = ITEMS.register("solidifier", () -> new BlockItem(SOLIDIFIER.get(), new Item.Properties().group(GooMod.ITEM_GROUP).maxStackSize(1)));
    public static final RegistryObject<TileEntityType<SolidifierTile>> SOLIDIFIER_TILE = TILES.register("solidifier", () -> TileEntityType.Builder.create(SolidifierTile::new, SOLIDIFIER.get()).build(null));

    // Goo!
    public static final RegistryObject<GooFluid> AQUATIC_GOO = FLUIDS.register("aquatic_goo", () -> new GooFluid(Resources.Still.AQUATIC_GOO, Resources.Flowing.AQUATIC_GOO));
    public static final RegistryObject<GooFluid> CHROMATIC_GOO = FLUIDS.register("chromatic_goo", () -> new GooFluid(Resources.Still.CHROMATIC_GOO, Resources.Flowing.CHROMATIC_GOO));
    public static final RegistryObject<GooFluid> CRYSTAL_GOO = FLUIDS.register("crystal_goo", () -> new GooFluid(Resources.Still.CRYSTAL_GOO, Resources.Flowing.CRYSTAL_GOO));
    public static final RegistryObject<GooFluid> DECAY_GOO = FLUIDS.register("decay_goo", () -> new GooFluid(Resources.Still.DECAY_GOO, Resources.Flowing.DECAY_GOO));
    public static final RegistryObject<GooFluid> EARTHEN_GOO = FLUIDS.register("earthen_goo", () -> new GooFluid(Resources.Still.EARTHEN_GOO, Resources.Flowing.EARTHEN_GOO));
    public static final RegistryObject<GooFluid> ENERGETIC_GOO = FLUIDS.register("energetic_goo", () -> new GooFluid(Resources.Still.ENERGETIC_GOO, Resources.Flowing.ENERGETIC_GOO));
    public static final RegistryObject<GooFluid> FAUNAL_GOO = FLUIDS.register("faunal_goo", () -> new GooFluid(Resources.Still.FAUNAL_GOO, Resources.Flowing.FAUNAL_GOO));
    public static final RegistryObject<GooFluid> FLORAL_GOO = FLUIDS.register("floral_goo", () -> new GooFluid(Resources.Still.FLORAL_GOO, Resources.Flowing.FLORAL_GOO));
    public static final RegistryObject<GooFluid> FUNGAL_GOO = FLUIDS.register("fungal_goo", () -> new GooFluid(Resources.Still.FUNGAL_GOO, Resources.Flowing.FUNGAL_GOO));
    public static final RegistryObject<GooFluid> HONEY_GOO = FLUIDS.register("honey_goo", () -> new GooFluid(Resources.Still.HONEY_GOO, Resources.Flowing.HONEY_GOO));
    public static final RegistryObject<GooFluid> LOGIC_GOO = FLUIDS.register("logic_goo", () -> new GooFluid(Resources.Still.LOGIC_GOO, Resources.Flowing.LOGIC_GOO));
    public static final RegistryObject<GooFluid> METAL_GOO = FLUIDS.register("metal_goo", () -> new GooFluid(Resources.Still.METAL_GOO, Resources.Flowing.METAL_GOO));
    public static final RegistryObject<GooFluid> MOLTEN_GOO = FLUIDS.register("molten_goo", () -> new GooFluid(Resources.Still.MOLTEN_GOO, Resources.Flowing.MOLTEN_GOO));
    public static final RegistryObject<GooFluid> OBSIDIAN_GOO = FLUIDS.register("obsidian_goo", () -> new GooFluid(Resources.Still.OBSIDIAN_GOO, Resources.Flowing.OBSIDIAN_GOO));
    public static final RegistryObject<GooFluid> REGAL_GOO = FLUIDS.register("regal_goo", () -> new GooFluid(Resources.Still.REGAL_GOO, Resources.Flowing.REGAL_GOO));
    public static final RegistryObject<GooFluid> SLIME_GOO = FLUIDS.register("slime_goo", () -> new GooFluid(Resources.Still.SLIME_GOO, Resources.Flowing.SLIME_GOO));
    public static final RegistryObject<GooFluid> SNOW_GOO = FLUIDS.register("snow_goo", () -> new GooFluid(Resources.Still.SNOW_GOO, Resources.Flowing.SNOW_GOO));
    public static final RegistryObject<GooFluid> VITAL_GOO = FLUIDS.register("vital_goo", () -> new GooFluid(Resources.Still.VITAL_GOO, Resources.Flowing.VITAL_GOO));
    public static final RegistryObject<GooFluid> WEIRD_GOO = FLUIDS.register("weird_goo", () -> new GooFluid(Resources.Still.WEIRD_GOO, Resources.Flowing.WEIRD_GOO));

    public static String getFluidTranslationKey(String key)
    {
        Fluid f = getFluid(key);
        if (f == null) {
            return null;
        }
        return f.getAttributes().getTranslationKey();
    }

    public static Fluid getFluid(String key)
    {
        RegistryObject<Fluid> fluid = FLUIDS.getEntries().stream().filter(f -> f.getId().toString().equals(key)).findFirst().orElse(null);
        if (fluid == null) {
            return null;
        }
        return fluid.get();
    }
}
