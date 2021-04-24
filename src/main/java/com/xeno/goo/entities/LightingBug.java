package com.xeno.goo.entities;

import com.xeno.goo.setup.Registry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.FlyingMovementController;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.Goal.Flag;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.fluid.Fluid;
import net.minecraft.network.PacketBuffer;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.ITag;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.village.PointOfInterest;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LightingBug extends AnimalEntity implements IFlyingAnimal, IEntityAdditionalSpawnData {

	public static AttributeModifierMap.MutableAttribute setCustomAttributes() {

		return MobEntity.func_233666_p_()
				.createMutableAttribute(Attributes.MAX_HEALTH, 10D)
				.createMutableAttribute(Attributes.FLYING_SPEED, 1.2F)
				.createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.5F)
				.createMutableAttribute(Attributes.ATTACK_DAMAGE, 1.0D)
				.createMutableAttribute(Attributes.FOLLOW_RANGE, 48.0D);
	}

	private BlockPos darkPosition;

	private GooSplat lightSplat;

	public LightingBug(EntityType<LightingBug> type, World worldIn) {

		super(type, worldIn);
		this.moveController = new FlyingMovementController(this, 20, true);
		this.setPathPriority(PathNodeType.DANGER_CACTUS, -1);
		this.setPathPriority(PathNodeType.DANGER_FIRE, -1);
		this.setPathPriority(PathNodeType.DANGER_OTHER, -1);
		this.setPathPriority(PathNodeType.UNPASSABLE_RAIL, 0);
		this.setPathPriority(PathNodeType.WATER, -1);
		this.setPathPriority(PathNodeType.WATER_BORDER, 16);
		this.setPathPriority(PathNodeType.COCOA, -1);
		this.setPathPriority(PathNodeType.FENCE, -1);
		setGrowingAge(-100000);
	}

	@Override
	protected PathNavigator createNavigator(World worldIn) {

		FlyingPathNavigator flyingpathnavigator = new FlyingPathNavigator(this, worldIn) {

			@Override
			public boolean canEntityStandOnPos(BlockPos pos) {

				return this.world.getBlockState(pos).isTopSolid(this.world, pos, this.entity, Direction.UP);
			}
		};
		flyingpathnavigator.setCanOpenDoors(false);
		flyingpathnavigator.setCanSwim(false);
		flyingpathnavigator.setCanEnterDoors(true);
		return flyingpathnavigator;
	}

	@Override
	protected void registerGoals() {

		//this.goalSelector.addGoal(0, new EnterNestGoal());
		//this.drinkGooGoal = new DrinkGooGoal();
		//this.goalSelector.addGoal(10, this.drinkGooGoal);
		this.goalSelector.addGoal(11, new LightDarkBlockGoal());
		this.goalSelector.addGoal(21, new MoveToDarkBlockGoal());
		this.goalSelector.addGoal(40, new WanderGoal());
		this.goalSelector.addGoal(45, new FindDarkBlockGoal());
		this.goalSelector.addGoal(50, new SwimGoal(this));
	}

	@Override
	public void writeSpawnData(PacketBuffer buffer) {

	}

	@Override
	public void readSpawnData(PacketBuffer additionalData) {

	}

	@Override
	public AgeableEntity createChild(ServerWorld world, AgeableEntity mate) {

		LightingBug childEntity = Registry.LIGHTING_BUG.get().create(world);
		//		if (mate instanceof LightingBug) {
		//			if (this.rand.nextBoolean()) {
		//				childEntity.setType(this.getType());
		//			} else {
		//				childEntity.setType(((LightingBug)mate).getType());
		//			}

		//			if (this.isTamed()) {
		//				childEntity.setOwnerId(this.getOwnerId());
		//				childEntity.setTamed(true);
		//			}
		//		}

		return childEntity;
	}

	public float getBodyPitch(float pct) {

		return MathHelper.lerp(pct, 0, 0);
	}

	@Override
	public CreatureAttribute getCreatureAttribute() {

		return CreatureAttribute.ARTHROPOD;
	}

	@Override
	protected void handleFluidJump(ITag<Fluid> fluidTag) {

		this.setMotion(this.getMotion().add(0.0D, 0.01D, 0.0D));
	}

	@Override
	protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {

		return this.isChild() ? sizeIn.height * 0.5F : sizeIn.height * 0.5F;
	}

	@Override
	public boolean onLivingFall(float distance, float damageMultiplier) {

		return false;
	}

	@Override
	protected void updateFallState(double y, boolean onGroundIn, BlockState state, BlockPos pos) {

	}

	@Override
	protected boolean makeFlySound() {

		return true;
	}

	@Override
	protected void updateMovementGoalFlags() {
		boolean flag = !(this.getControllingPassenger() instanceof MobEntity);
		boolean flag1 = !(this.getRidingEntity() instanceof BoatEntity);
		this.goalSelector.setFlag(Flag.JUMP, flag && flag1);
		this.goalSelector.setFlag(Flag.LOOK, flag);
	}

	class DrinkGooGoal extends Goal {

		private final FluidStack DRAINED_LIQUID_PER_SIP = new FluidStack(Registry.RADIANT_GOO.get(), 1);
		private boolean running;
		private Vector3d nextTarget;
		private int ticks = 0;

		DrinkGooGoal() {

			this.setMutexFlags(EnumSet.of(Flag.MOVE, Flag.TARGET));
		}

		@Override
		public boolean shouldExecute() {
			//			if (GooBee.this.remainingCooldownBeforeLocatingTrough > 0) {
			//				return false;
			//			} else if (GooBee.this.hasEnoughGoo()) {
			//				return false;
			//			} else if (rand.nextFloat() < 0.7F) {
			//				return false;
			//			} else {
			//				List<BlockPos> troughOptions = this.getTroughs();
			//				for(BlockPos troughOption : troughOptions) {
			//					IFluidHandler troughHandler = GooBee.troughHandlerFromPosition(world, troughOption);
			//					if (troughHandler == null) {
			//						continue;
			//					}
			//					GooBee.this.savedTroughPos = troughOption;
			//					GooBee.this.navigator.tryMoveToXYZ((double) GooBee.this.savedTroughPos.getX() + 0.5D,
			//							(double) GooBee.this.savedTroughPos.getY() + 0.5D,
			//							(double) GooBee.this.savedTroughPos.getZ() + 0.5D, 1.2D);
			//					return true;
			//				}
			//			}
			return false;
		}

		@Override
		public boolean shouldContinueExecuting() {

			if (!this.running) {
				return false;
			} else
			//				if (!GooBee.this.hasTrough()) {
			//				return false;
			//			} else if (this.completedDrinkingGoo()) {
			//				return false;
			//			} else if (ticksExisted % 20 == 0 && !GooBee.this.isTrough(GooBee.this.savedTroughPos)) {
			//				GooBee.this.clearTrough();
			//				return false;
			//			} else
			{
				return true;
			}
		}
		//
		//		private boolean completedDrinkingGoo() {
		//			return !GooBee.this.goo.isEmpty() && GooBee.this.goo.getAmount() >= GOO_DELIVERY_AMOUNT;
		//		}

		private boolean isRunning() {

			return this.running;
		}

		private void cancel() {

			this.running = false;
		}

		/**
		 * Execute a one shot task or start executing a continuous task
		 */
		public void startExecuting() {

			this.ticks = 0;
			this.running = true;
			//GooBee.this.resetTicksWithoutDrinkingGoo();
		}

		/**
		 * Reset the task's internal state. Called when this task is interrupted by another one
		 */
		public void resetTask() {

			this.running = false;
			//GooBee.this.navigator.clearPath();
			//GooBee.this.remainingCooldownBeforeLocatingTrough = 200;
		}

		/**
		 * Keep ticking a continuous task that has already been started
		 */
		public void tick() {

			++this.ticks;
			if (this.ticks > 600) {
				//GooBee.this.clearTrough();
			} else {
				Vector3d vector3d = //Vector3d.copyCenteredHorizontally(GooBee.this.savedTroughPos)
						new Vector3d(0, 0, 0).add(0.0D, 0.3D, 0.0D);
				if (vector3d.distanceTo(getPositionVec()) > 0.7D) {
					this.nextTarget = vector3d;
					this.moveToNextTarget();
				} else {
					if (this.nextTarget == null) {
						this.nextTarget = vector3d;
					}
					double distanceToTarget = getPositionVec().distanceTo(this.nextTarget);

					boolean isVeryCloseToTarget = distanceToTarget <= 0.2D;
					boolean isCloseEnoughToTarget = distanceToTarget <= 0.7D;
					boolean isDerping = true;
					// it seems to be taking more than 30 seconds to get to our trough.
					if (!isVeryCloseToTarget && this.ticks > 600) {
						//GooBee.this.clearTrough();
					} else {
						// we're at the trough. Presuming it is valid, we drink from it.
						if (isVeryCloseToTarget) {
							boolean tryDerping = rand.nextInt(25) == 0;
							if (tryDerping) {
								this.nextTarget = new Vector3d(vector3d.getX() + (double) this.getRandomOffset(), vector3d.getY(),
										vector3d.getZ() + (double) this.getRandomOffset());
								navigator.clearPath();
							} else {
								isDerping = false;
							}
							getLookController().setLookPosition(vector3d.getX(), vector3d.getY(), vector3d.getZ());
						}

						if (isDerping) {
							this.moveToNextTarget();
						}

						if (isCloseEnoughToTarget) {
							//							IFluidHandler fh = GooBee.troughHandlerFromPosition(world, GooBee.this.savedTroughPos);
							//							if (fh != null && world.rand.nextInt(6) == 0) {
							//								if (fh.getFluidInTank(0).getAmount() >= DRAINED_LIQUID_PER_SIP.getAmount()) {
							//									if (!this.completedDrinkingGoo()) {
							//										GooBee.this.fill(fh.drain(DRAINED_LIQUID_PER_SIP, IFluidHandler.FluidAction.EXECUTE), FluidAction.EXECUTE);
							//									}
							//								} else {
							//									// trough is empty, doesn't have the goo we want.
							//									GooBee.this.clearTrough();
							//								}
							//							}
						}
					}
				}
			}
		}

		private void moveToNextTarget() {

			getMoveHelper().setMoveTo(this.nextTarget.getX(), this.nextTarget.getY(), this.nextTarget.getZ(), (double) 0.7F);
		}

		private float getRandomOffset() {

			return (rand.nextFloat() * 2.0F - 1.0F) * 0.33333334F;
		}

		private List<BlockPos> getTroughs() {

			return this.findTrough(32.0D);
		}

		private List<BlockPos> findTrough(double distance) {

			BlockPos blockpos = getPosition();
			PointOfInterestManager pointofinterestmanager = ((ServerWorld) world).getPointOfInterestManager();
			Stream<PointOfInterest> stream = pointofinterestmanager
					.func_219146_b((poit) -> poit == Registry.GOO_TROUGH_POI.get(),
							blockpos, 20, PointOfInterestManager.Status.ANY);
			return stream.map(PointOfInterest::getPos)
					//.filter(GooBee.this::isTrough)
					.sorted(Comparator.comparingDouble((destBp) -> destBp.distanceSq(blockpos)))
					.collect(Collectors.toList());
		}
	}

	class MoveToDarkBlockGoal extends WanderGoal {

		MoveToDarkBlockGoal() {

			this.setMutexFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean shouldExecute() {

			if (lightSplat != null) {
				if (!lightSplat.isAlive())
					lightSplat = null;
				else
					return super.shouldExecute();
			}
			return darkPosition != null && super.shouldExecute();
		}

		@Override
		public void startExecuting() {

			goalSelector.disableFlag(Flag.TARGET);
			if (lightSplat != null) {
				navigator.setPath(navigator.getPathToPos(lightSplat.getPosition(), 0), 1.0D);
			} else
				navigator.setPath(navigator.getPathToPos(new BlockPos(darkPosition), 0), 1.0D);
		}

		@Override
		public void resetTask() {

			goalSelector.enableFlag(Flag.TARGET);
			darkPosition = null;
		}
	}

	class LightDarkBlockGoal extends Goal {

		private LightDarkBlockGoal() {

		}

		@Override
		public boolean shouldExecute() {

			if (lightSplat != null) {
				if (!lightSplat.isAlive())
					lightSplat = null;
				else
					return getPosition().equals(lightSplat.getPosition());
			}
			return (darkPosition != null && getPosition().equals(darkPosition));
		}

		@Override
		public boolean shouldContinueExecuting() {

			return lightSplat != null && lightSplat.isAlive();
		}

		@Override
		public void startExecuting() {

			final BlockPos start = getPosition();

			goalSelector.disableFlag(Flag.MOVE);
			navigator.tryMoveToXYZ(start.getX(), start.getY(), start.getZ(), 1);
			navigator.clearPath();
			if (shouldContinueExecuting())
				return;
			World world = getEntityWorld();
			for (Direction dir : Direction.getFacingDirections(LightingBug.this)) {
				BlockPos pos = start.offset(dir);
				BlockState state = world.getBlockState(pos);
				if (state.isSolidSide(world, pos, dir.getOpposite())) {
					lightSplat = new GooSplat(Registry.GOO_SPLAT.get(), LightingBug.this, world,
							new FluidStack(Registry.RADIANT_GOO.get(), 1),
							// add half the direction vector to the centered position to align to the center of the face
							Vector3d.copyCentered(start).add(Vector3d.copy(dir.getDirectionVec()).scale(0.5)),
							pos, dir.getOpposite(),
							true, 0, false);
					world.addEntity(lightSplat);
					return;
				}
			}
		}

		@Override
		public void tick() {

			if (lightSplat != null && lightSplat.isAtRest())
				GooSplat.getGoo(lightSplat).fill(new FluidStack(Registry.RADIANT_GOO.get(), 1), FluidAction.EXECUTE);
		}

		@Override
		public void resetTask() {

			goalSelector.enableFlag(Flag.MOVE);
			if (lightSplat != null && !lightSplat.isAlive())
				lightSplat = null;
		}
	}

	class FindDarkBlockGoal extends Goal {

		private FindDarkBlockGoal() {

			this.setMutexFlags(EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean shouldExecute() {

			return darkPosition == null && rand.nextInt(10) == 0;
		}

		@Override
		public boolean shouldContinueExecuting() {

			return false;
		}

		@Override
		public void tick() {

		}

		@Override
		public void startExecuting() {

			Vector3d position;
			//			if (LightingBug.this.isNestValid() && !LightingBug.this.isWithinDistance(LightingBug.this.nestPos, 22)) {
			//				Vector3d vector3d1 = Vector3d.copyCentered(LightingBug.this.nestPos);
			//				position = vector3d1.subtract(LightingBug.this.getPositionVec()).normalize();
			//			} else
			{
				position = LightingBug.this.getLook(0.0F);
			}

			Vector3d airTarget = RandomPositionGenerator.findAirTarget(LightingBug.this,
					8, 7, position, ((float) Math.PI / 2F), 2, 1);
			Vector3d target = airTarget != null ? airTarget : RandomPositionGenerator.findGroundTarget(LightingBug.this,
					8, 4, -2, position, ((float) Math.PI / 2F));
			if (target != null) {
				BlockPos start = new BlockPos(target);
				World world = getEntityWorld();
				BlockState state = world.getBlockState(start);
				if (!state.isAir(world, start)) {
					return;
				}
				for (Direction dir : Direction.getFacingDirections(LightingBug.this)) {
					BlockPos pos = start.offset(dir);
					state = world.getBlockState(pos);
					if (state.isSolidSide(world, pos, dir.getOpposite())) {
						darkPosition = start;
						return;
					}
				}
			}
		}
	}

	class WanderGoal extends Goal {

		WanderGoal() {

			this.setMutexFlags(EnumSet.of(Flag.MOVE));
		}

		/**
		 * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
		 * method as well.
		 */
		@Override
		public boolean shouldExecute() {

			return navigator.noPath() && rand.nextInt(10) == 0;
		}

		/**
		 * Returns whether an in-progress EntityAIBase should continue executing
		 */
		@Override
		public boolean shouldContinueExecuting() {

			return navigator.hasPath();
		}

		/**
		 * Execute a one shot task or start executing a continuous task
		 */
		@Override
		public void startExecuting() {

			Vector3d vector3d = this.getRandomLocation();
			if (vector3d != null) {
				navigator.setPath(navigator.getPathToPos(new BlockPos(vector3d), 2), 0.75D);
			}
		}

		@Nullable
		private Vector3d getRandomLocation() {

			Vector3d direction;
			//			if (LightingBug.this.isNestValid() && !LightingBug.this.isWithinDistance(LightingBug.this.nestPos, 22)) {
			//				Vector3d vector3d1 = Vector3d.copyCentered(LightingBug.this.nestPos);
			//				direction = vector3d1.subtract(LightingBug.this.getPositionVec()).normalize();
			//			} else
			{
				direction = getLook(0.0F);
			}

			Vector3d airTarget = RandomPositionGenerator.
					findAirTarget(LightingBug.this, 8, 7, direction, ((float) Math.PI / 2F), 2, 1);
			return airTarget != null ? airTarget : RandomPositionGenerator.
					findGroundTarget(LightingBug.this, 8, 4, -2, direction, ((float) Math.PI / 2F));
		}
	}
}