package com.xeno.goo.client.models;

import com.google.common.collect.*;
import com.mojang.datafixers.util.Pair;
import com.xeno.goo.items.GooHolder;
import com.xeno.goo.setup.Registry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.*;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.fluids.FluidUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public final class DynamicCrucibleModel implements IModelGeometry<DynamicCrucibleModel>
{
    private static final Logger LOGGER = LogManager.getLogger();

    // minimal Z offset to prevent depth-fighting
    private static final float NORTH_Z_COVER = 7.496f / 16f;
    private static final float SOUTH_Z_COVER = 8.504f / 16f;
    private static final float NORTH_Z_FLUID = 7.498f / 16f;
    private static final float SOUTH_Z_FLUID = 8.502f / 16f;

    @Nonnull
    private final Fluid fluid;

    private final boolean flipGas;
    private final boolean tint;
    private final boolean coverIsMask;
    private final boolean applyFluidLuminosity;

    @Deprecated
    public DynamicCrucibleModel(Fluid fluid, boolean flipGas, boolean tint, boolean coverIsMask)
    {
        this(fluid, flipGas, tint, coverIsMask, true);
    }

    public DynamicCrucibleModel(Fluid fluid, boolean flipGas, boolean tint, boolean coverIsMask, boolean applyFluidLuminosity)
    {
        this.fluid = fluid;
        this.flipGas = flipGas;
        this.tint = tint;
        this.coverIsMask = coverIsMask;
        this.applyFluidLuminosity = applyFluidLuminosity;
    }

    /**
     * Returns a new ModelDynBucket representing the given fluid, but with the same
     * other properties (flipGas, tint, coverIsMask).
     */
    public DynamicCrucibleModel withFluid(Fluid newFluid)
    {
        return new DynamicCrucibleModel(newFluid, flipGas, tint, coverIsMask, applyFluidLuminosity);
    }

    @Override
    public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList overrides, ResourceLocation modelLocation)
    {
        RenderMaterial particleLocation = owner.isTexturePresent("particle") ? owner.resolveTexture("particle") : null;
        RenderMaterial baseLocation = owner.isTexturePresent("base") ? owner.resolveTexture("base") : null;
        RenderMaterial fluidMaskLocation = owner.isTexturePresent("fluid") ? owner.resolveTexture("fluid") : null;
        RenderMaterial coverLocation = owner.isTexturePresent("fluid") ? owner.resolveTexture("cover") : null;

        IModelTransform transformsFromModel = owner.getCombinedTransform();

        TextureAtlasSprite fluidSprite = fluid != Fluids.EMPTY ? spriteGetter.apply(ForgeHooksClient.getBlockMaterial(fluid.getAttributes().getStillTexture())) : null;
        TextureAtlasSprite coverSprite = (coverLocation != null && (!coverIsMask || baseLocation != null)) ? spriteGetter.apply(coverLocation) : null;

        ImmutableMap<TransformType, TransformationMatrix> transformMap =
                PerspectiveMapWrapper.getTransforms(new ModelTransformComposition(transformsFromModel, modelTransform));

        TextureAtlasSprite particleSprite = particleLocation != null ? spriteGetter.apply(particleLocation) : null;

        if (particleSprite == null) particleSprite = fluidSprite;
        if (particleSprite == null && !coverIsMask) particleSprite = coverSprite;

        // if the fluid is lighter than air, will manipulate the initial state to be rotated 180deg to turn it upside down
        if (flipGas && fluid != Fluids.EMPTY && fluid.getAttributes().isLighterThanAir()) {
            modelTransform = new SimpleModelTransform(
                    modelTransform.getRotation().blockCornerToCenter().composeVanilla(
                            new TransformationMatrix(null, new Quaternion(0, 0, 1, 0), null, null)).blockCenterToCorner());
        }

        TransformationMatrix transform = modelTransform.getRotation();

        ItemMultiLayerBakedModel.Builder builder = ItemMultiLayerBakedModel.builder(owner, particleSprite, new DynamicCrucibleModel.ContainedFluidOverrideHandler(overrides, bakery, owner, this), transformMap);

        if (baseLocation != null) {
            // build base (insidest)
            builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemLayerModel.getQuadsForSprites(ImmutableList.of(baseLocation), transform, spriteGetter));
        }

        if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = spriteGetter.apply(fluidMaskLocation);
            if (templateSprite != null) {
                // build liquid layer (inside)
                int luminosity = applyFluidLuminosity ? fluid.getAttributes().getLuminosity() : 0;
                int color = tint ? fluid.getAttributes().getColor() : 0xFFFFFFFF;
                builder.addQuads(ItemLayerModel.getLayerRenderType(luminosity > 0), ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, NORTH_Z_FLUID, Direction.NORTH, color, 1, luminosity));
                builder.addQuads(ItemLayerModel.getLayerRenderType(luminosity > 0), ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, SOUTH_Z_FLUID, Direction.SOUTH, color, 1, luminosity));
            }
        }

        if (coverIsMask) {
            if (coverSprite != null && baseLocation != null) {
                TextureAtlasSprite baseSprite = spriteGetter.apply(baseLocation);
                builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemTextureQuadConverter.convertTexture(transform, coverSprite, baseSprite, NORTH_Z_COVER, Direction.NORTH, 0xFFFFFFFF, 2));
                builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemTextureQuadConverter.convertTexture(transform, coverSprite, baseSprite, SOUTH_Z_COVER, Direction.SOUTH, 0xFFFFFFFF, 2));
            }
        } else {
            if (coverSprite != null) {
                builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemTextureQuadConverter.genQuad(transform, 0, 0, 16, 16, NORTH_Z_COVER, coverSprite, Direction.NORTH, 0xFFFFFFFF, 2));
                builder.addQuads(ItemLayerModel.getLayerRenderType(false), ItemTextureQuadConverter.genQuad(transform, 0, 0, 16, 16, SOUTH_Z_COVER, coverSprite, Direction.SOUTH, 0xFFFFFFFF, 2));
            }
        }

        builder.setParticle(particleSprite);

        return builder.build();
    }

    @Override
    public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
    {
        Set<RenderMaterial> texs = Sets.newHashSet();

        if (owner.isTexturePresent("particle")) texs.add(owner.resolveTexture("particle"));
        if (owner.isTexturePresent("base")) texs.add(owner.resolveTexture("base"));
        if (owner.isTexturePresent("fluid")) texs.add(owner.resolveTexture("fluid"));
        if (owner.isTexturePresent("cover")) texs.add(owner.resolveTexture("cover"));

        return texs;
    }


    private static final class ContainedFluidOverrideHandler extends ItemOverrideList
    {
        private final Map<String, IBakedModel> cache = Maps.newHashMap(); // contains all the baked models since they'll never change
        private final ItemOverrideList nested;
        private final ModelBakery bakery;
        private final IModelConfiguration owner;
        private final DynamicCrucibleModel parent;

        private ContainedFluidOverrideHandler(ItemOverrideList nested, ModelBakery bakery, IModelConfiguration owner, DynamicCrucibleModel parent)
        {
            this.nested = nested;
            this.bakery = bakery;
            this.owner = owner;
            this.parent = parent;
        }

        @Override
        public IBakedModel func_239290_a_(IBakedModel originalModel, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity)
        {
            IBakedModel overridden = nested.func_239290_a_(originalModel, stack, world, entity);
            if (overridden != originalModel) return overridden;

            GooHolder holder = GooHolder.read(stack);
            String selected = holder.selected();
            Fluid fluid = Registry.getFluid(selected);

            if (fluid == null || fluid == Fluids.EMPTY) {
                return originalModel;
            }

            if (!cache.containsKey(selected))
            {
                DynamicCrucibleModel unbaked = this.parent.withFluid(fluid);
                IBakedModel bakedModel = unbaked.bake(owner, bakery, ModelLoader.defaultTextureGetter(), ModelRotation.X0_Y0, this, new ResourceLocation("goo:crucible_override"));
                cache.put(selected, bakedModel);
                return bakedModel;
            }

            return cache.get(selected);
        }
    }
}