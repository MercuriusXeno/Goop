package com.xeno.goo.interactions;

import com.xeno.goo.fluids.GooFluid;
import com.xeno.goo.library.AudioHelper;
import com.xeno.goo.setup.Registry;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.function.Supplier;

public class Regal
{
    private static final Supplier<GooFluid> fluidSupplier = Registry.REGAL_GOO;
    private static final int diamondHarvestLevel = 3;
    private static final int bedrockHardness = -1;
    private static final ItemStack mockPick = new ItemStack(Items.DIAMOND_PICKAXE, 1);
    static {
        mockPick.addEnchantment(Enchantments.FORTUNE, 3);
    }
    public static void registerInteractions()
    {
        GooInteractions.registerSplat(fluidSupplier.get(), "regal_breaker", Regal::breaker, Regal::isValidForHarvest);

        GooInteractions.registerBlobHit(fluidSupplier.get(), "regal_hit", Regal::entityHit);
    }

    private static boolean entityHit(BlobHitContext c) {

        c.damageVictim(5f);
        // find nearby revenge target
        List<LivingEntity> entities = c.world().getEntitiesWithinAABB(LivingEntity.class, c.victim().getBoundingBox().grow(12), e -> e != c.victim() && !(e instanceof PlayerEntity));
        if (!entities.isEmpty()) {
            LivingEntity closest = entities.stream().min((e1, e2) -> findNearestEntity(c.victim(), e1, e2)).get();
            c.victim().setRevengeTarget(closest);
            c.victim().setAttackingPlayer(null);
            c.victim().setLastAttackedEntity(closest);
        }

        c.knockback(1f);
        return true;
    }

    private static int findNearestEntity(LivingEntity victim, LivingEntity livingEntity, LivingEntity livingEntity1) {
        float distance1 = livingEntity.getDistance(victim);
        float distance2 = livingEntity1.getDistance(victim);
        return Float.compare(distance1, distance2);
    }

    private static boolean isValidForHarvest(SplatContext context) {
        BlockPos blockPos = context.blockPos();
        BlockState state = context.world().getBlockState(blockPos);
        return !state.getMaterial().isLiquid() && state.getHarvestLevel() <= diamondHarvestLevel && state.getBlockHardness(context.world(), blockPos) != bedrockHardness;
    }

    private static boolean breaker(SplatContext context)
    {
        if ((context.world() instanceof ServerWorld)) {
            BlockPos blockPos = context.blockPos();
            BlockState state = context.world().getBlockState(blockPos);
            Vector3d dropPos = Vector3d.copy(blockPos).add(0.5d, 0.5d, 0.5d);
            SoundType breakAudio = state.getBlock().getSoundType(state, context.world(), blockPos, null);
            AudioHelper.headlessAudioEvent(context.world(), blockPos, breakAudio.getBreakSound(), SoundCategory.BLOCKS,
                breakAudio.volume, () -> breakAudio.pitch);
            ((ServerWorld)context.world()).spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, state), dropPos.x, dropPos.y, dropPos.z, 12, 0d, 0d, 0d, 0.15d);
            LootContext.Builder lootBuilder = new LootContext.Builder((ServerWorld) context.world());
            List<ItemStack> drops = state.getDrops(lootBuilder
                    .withNullableParameter(LootParameters.THIS_ENTITY, context.splat().owner())
                    .withParameter(LootParameters.ORIGIN, context.blockCenterVec())
                    .withParameter(LootParameters.TOOL, mockPick)
            );
            boolean hasChanges = context.world().removeBlock(blockPos, false);
            if (!hasChanges) {
                return false;
            }

            drops.forEach((d) -> context.world().addEntity(
                    new ItemEntity(context.world(), dropPos.getX(), dropPos.getY(), dropPos.getZ(), d)
            ));
        }

        return true;
    }
}
