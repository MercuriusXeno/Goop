package com.xeno.goo.tiles;

import com.xeno.goo.library.MixerRecipe;
import com.xeno.goo.library.MixerRecipes;
import com.xeno.goo.network.FluidUpdatePacket;
import com.xeno.goo.network.Networking;
import com.xeno.goo.setup.Registry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MixerTile extends TileEntity implements ITickableTileEntity, FluidUpdatePacket.IFluidPacketReceiver
{
    private MixerFluidHandler eastHandler = createHandler(Direction.EAST);
    private MixerFluidHandler westHandler = createHandler(Direction.WEST);
    private LazyOptional<MixerFluidHandler> eastLazy = LazyOptional.of(() -> eastHandler);
    private LazyOptional<MixerFluidHandler> westLazy = LazyOptional.of(() -> westHandler);
    List<FluidStack> goo = new ArrayList<>();

    public MixerTile()
    {
        super(Registry.MIXER_TILE.get());
        while(goo.size() < 2) {
            goo.add(FluidStack.EMPTY);
        }
    }

    public FluidStack goo(Direction side)
    {
        int sideTank = sideTank(side);
        if (sideTank == -1) {
            return FluidStack.EMPTY;
        }
        return goo.get(sideTank);
    }

    public FluidStack goo(Direction side, Fluid fluid)
    {
        FluidStack maybeGoo = goo(side);
        if (maybeGoo.getFluid().equals(fluid)) {
            return maybeGoo;
        }

        return FluidStack.EMPTY;
    }

    public boolean hasFluid(Direction side, Fluid fluid)
    {
        return goo(side, fluid) != FluidStack.EMPTY;
    }

    private int sideTank(Direction side)
    {
        // get rotated side and then determine east/west (or upward, which doesn't change) orientation.
        if (side == orientedRight()) {
            return 0;
        } else if (side == orientedLeft()) {
            return 1;
        }

        return -1;
    }

    public Direction orientedRight() {
        switch(this.facing()) {
            case NORTH:
                return Direction.EAST;
            case SOUTH:
                return Direction.WEST;
            case EAST:
                return Direction.SOUTH;
            case WEST:
                return Direction.NORTH;
        }
        return Direction.EAST;
    }

    public Direction orientedLeft() {
        switch(this.facing()) {
            case NORTH:
                return Direction.WEST;
            case SOUTH:
                return Direction.EAST;
            case EAST:
                return Direction.NORTH;
            case WEST:
                return Direction.SOUTH;
        }
        return Direction.WEST;
    }

    public Direction facing()
    {
        return this.getBlockState().get(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public void updateFluidsTo(List<FluidStack> fluids)
    {
        this.goo = fluids;
    }

    @Override
    public void tick()
    {
        if (world == null || world.isRemote) {
            return;
        }

        tryPushingRecipeResult();
    }

    private boolean hasValidGooInputs()
    {
        return getRecipeFromInputs() != null;
    }

    private MixerRecipe getRecipeFromInputs()
    {
        return MixerRecipes.getRecipe(goo.get(0), goo.get(1));
    }

    private void tryPushingRecipeResult() {
        MixerRecipe recipe = getRecipeFromInputs();
        if (recipe == null) {
            return;
        }

        // check to make sure the recipe inputs amounts are satisfied.
        if (!isRecipeSatisfied(recipe)) {
            return;
        }

        IFluidHandler cap = tryGettingFluidCapabilityFromTileBelow();
        if (cap == null) {
            return;
        }

        int sentResult = cap.fill(recipe.output(), IFluidHandler.FluidAction.SIMULATE);
        if (sentResult == 0 || sentResult < recipe.output().getAmount()) {
            return;
        }

        deductInputQuantities(recipe.inputs());

        cap.fill(recipe.output(), IFluidHandler.FluidAction.EXECUTE);
    }

    private boolean deductInputQuantities(Map<Fluid, Integer> inputs)
    {
        for(Map.Entry<Fluid, Integer> e : inputs.entrySet()) {
            FluidStack input = new FluidStack(e.getKey(), e.getValue());
            // try deducting from either tank. it doesn't really matter which we check first
            // simulate will tell us that it contains it or doesn't.
            if (eastHandler.drain(input, IFluidHandler.FluidAction.SIMULATE).isEmpty()) {
                // east handler doesn't have it.
                westHandler.drain(input, IFluidHandler.FluidAction.EXECUTE);
            } else {
                eastHandler.drain(input, IFluidHandler.FluidAction.EXECUTE);
            }
        }
        return true;
    }

    private boolean isRecipeSatisfied(MixerRecipe recipe)
    {
        for(Map.Entry<Fluid, Integer> e : recipe.inputs().entrySet()) {
            if (goo.stream().noneMatch(g -> g.getFluid() == e.getKey() && g.getAmount() >= e.getValue())) {
                return false;
            }
        }
        return true;
    }

    private IFluidHandler tryGettingFluidCapabilityFromTileBelow()
    {
        TileEntity tile = FluidHandlerHelper.tileAtDirection(this, Direction.DOWN);
        if (tile == null) {
            return null;
        }
        return FluidHandlerHelper.capability(tile, Direction.UP);
    }

    public void addGoo(Direction side, FluidStack fluidStack)
    {
        int sideTank = sideTank(side);
        if (sideTank == -1) {
            return;
        }
        goo.set(sideTank, fluidStack);
    }

    public void onContentsChanged() {
        if (world == null) {
            return;
        }
        if (!world.isRemote) {
            if (world.getServer() == null) {
                return;
            }
            Networking.sendToClientsAround(new FluidUpdatePacket(world.func_234923_W_(), pos, goo), Objects.requireNonNull(Objects.requireNonNull(world.getServer()).getWorld(world.func_234923_W_())), pos);
        }
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        return this.write(new CompoundNBT());
    }

    private CompoundNBT serializeGoo()  {
        CompoundNBT tag = new CompoundNBT();
        tag.putInt("count", goo.size());
        int index = 0;
        for(FluidStack s : goo) {
            CompoundNBT gooTag = new CompoundNBT();
            s.writeToNBT(gooTag);
            tag.put("goo" + index, gooTag);
            index++;
        }
        return tag;
    }

    private void deserializeGoo(CompoundNBT tag) {
        List<FluidStack> tagGooList = new ArrayList<>();
        int size = tag.getInt("count");
        for(int i = 0; i < size; i++) {
            CompoundNBT gooTag = tag.getCompound("goo" + i);
            FluidStack stack = FluidStack.loadFluidStackFromNBT(gooTag);
            if (stack.isEmpty()) {
                continue;
            }
            tagGooList.add(stack);
        }

        goo = tagGooList;
        while(goo.size() < 2) {
            goo.add(FluidStack.EMPTY);
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag.put("goo", serializeGoo());
        return super.write(tag);
    }

    public void read(BlockState state, CompoundNBT tag)
    {
        CompoundNBT gooTag = tag.getCompound("goo");
        deserializeGoo(gooTag);
        super.read(state, tag);
        onContentsChanged();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (side == orientedLeft()) {
                return westLazy.cast();
            }
            if (side == orientedRight()) {
                return eastLazy.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    private MixerFluidHandler createHandler(Direction d) {
        return new MixerFluidHandler(this, d);
    }

    public ItemStack mixerStack(Block block) {
        ItemStack stack = new ItemStack(block);

        CompoundNBT bulbTag = new CompoundNBT();
        write(bulbTag);
        bulbTag.remove("x");
        bulbTag.remove("y");
        bulbTag.remove("z");

        CompoundNBT stackTag = new CompoundNBT();
        stackTag.put("BlockEntityTag", bulbTag);
        stack.setTag(stackTag);

        return stack;
    }

    public static List<FluidStack> deserializeGooForDisplay(CompoundNBT tag) {
        List<FluidStack> tagGooList = new ArrayList<>();
        int size = tag.getInt("count");
        for(int i = 0; i < size; i++) {
            CompoundNBT gooTag = tag.getCompound("goo" + i);
            FluidStack stack = FluidStack.loadFluidStackFromNBT(gooTag);
            tagGooList.add(stack);
        }

        return tagGooList;
    }

    public int getSpaceRemaining(Direction side, FluidStack stack)
    {
        int sideTank = sideTank(side);
        if (sideTank == -1) {
            return 0;
        }
        IFluidHandler handler = orientedLeft() == side ? westHandler :
                (orientedRight() == side ? eastHandler : null);
        if (handler == null) {
            return 0;
        }
        // there may be space but this is the wrong kind of goo and you can't mix inside input tanks.
        if (!goo.get(sideTank).isEmpty() && !goo.get(sideTank).getFluid().equals(stack.getFluid())) {
            return 0;
        }
        return handler.getTankCapacity(0) - goo.get(sideTank).getAmount();
    }
}
