package com.xeno.goo.setup;

import com.xeno.goo.GooMod;
import com.xeno.goo.client.models.CrucibleModelLoader;
import com.xeno.goo.client.render.GooBulbRenderer;
import com.xeno.goo.client.render.SolidifierTileRenderer;
import com.xeno.goo.network.Networking;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ClientSetup
{
    public static void init(final FMLClientSetupEvent event)
    {
        // rendering stuff
        RenderTypeLookup.setRenderLayer(Registry.GOO_BULB.get(), RenderType.getCutout());
        GooBulbRenderer.register();
        SolidifierTileRenderer.register();
        ModelLoaderRegistry.registerLoader(new ResourceLocation(GooMod.MOD_ID, "crucible"), CrucibleModelLoader.INSTANCE);
    }
}
