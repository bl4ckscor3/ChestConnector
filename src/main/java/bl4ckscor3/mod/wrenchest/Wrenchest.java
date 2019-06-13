package bl4ckscor3.mod.wrenchest;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Items;
import net.minecraft.state.properties.ChestType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod(Wrenchest.MODID)
@EventBusSubscriber(bus=Bus.MOD)
public class Wrenchest
{
	public static final String MODID = "wrenchest";

	@SubscribeEvent
	public static void onRegisterItem(RegistryEvent.Register<Item> event)
	{
		event.getRegistry().register(new Item(new Item.Properties().group(ItemGroup.TOOLS).maxStackSize(1).defaultMaxDamage(256)) {
			@Override
			public boolean getIsRepairable(ItemStack toRepair, ItemStack repair)
			{
				return toRepair.getItem() == this && (repair.getItem() == this || repair.getItem() == Items.IRON_INGOT);
			}

			@Override
			public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext ctx)
			{
				ActionResultType result = checkConnections(stack, ctx);

				if(result == ActionResultType.SUCCESS && !ctx.getPlayer().isCreative())
					stack.damageItem(1, ctx.getPlayer(), p -> {});

				return result;
			}

			/**
			 * Checks which way two chests might be facing each other and then connects them
			 * @see Item#onItemUseFirst
			 */
			private ActionResultType checkConnections(ItemStack stack, ItemUseContext ctx)
			{
				if(!(ctx.getWorld().getBlockState(ctx.getPos()).getBlock() instanceof ChestBlock))
					return ActionResultType.PASS;

				World world = ctx.getWorld();
				BlockPos pos = ctx.getPos();
				BlockState chestState = world.getBlockState(pos);

				if(chestState.get(ChestBlock.TYPE) != ChestType.SINGLE) //disconnect double chests
				{
					Direction facingTowardsOther = ChestBlock.getDirectionToAttached(chestState);

					world.setBlockState(pos, chestState.with(ChestBlock.TYPE, ChestType.SINGLE));
					world.setBlockState(pos.offset(facingTowardsOther), world.getBlockState(pos.offset(facingTowardsOther)).with(ChestBlock.TYPE, ChestType.SINGLE));
					return ActionResultType.SUCCESS;
				}
				else if(ctx.getFace() != Direction.UP && ctx.getFace() != Direction.DOWN) //connect single chests, UP/DOWN check is here so double chests can be disconected by clicking on all faces
				{
					BlockPos otherPos = pos.offset(ctx.getFace());
					BlockState otherState = world.getBlockState(otherPos);

					if(otherState.getBlock() instanceof ChestBlock && otherState.get(ChestBlock.TYPE) == ChestType.SINGLE)
					{
						Direction facing = chestState.get(ChestBlock.FACING);
						Direction otherFacing = otherState.get(ChestBlock.FACING);

						//the chests are facing (away from) each other
						if((ctx.getFace() == facing || ctx.getFace() == otherFacing) && facing.getOpposite() == otherFacing)
						{
							if(connectChests(world, pos, otherPos, chestState, otherState, ctx.getFace(), facing, ctx.func_221532_j().x % 1, ctx.func_221532_j().z % 1, true))
								return ActionResultType.SUCCESS;
							else return ActionResultType.PASS;
						}
						//the clicked chest has the other chest to its left/right
						else if(ctx.getFace().rotateY() == facing || ctx.getFace().rotateYCCW() == facing)
						{
							if(connectChests(world, pos, otherPos, chestState, otherState, ctx.getFace(), facing, ctx.func_221532_j().x % 1, ctx.func_221532_j().z % 1, false))
								return ActionResultType.SUCCESS;
							else return ActionResultType.PASS;
						}
						//the clicked chest has its neighbor to the front/back
						else if(ctx.getFace().rotateY() == otherFacing || ctx.getFace().rotateYCCW() == otherFacing)
						{
							if(connectChests(world, pos, otherPos, chestState, otherState, ctx.getFace(), otherFacing, ctx.func_221532_j().x % 1, ctx.func_221532_j().z % 1, false))
								return ActionResultType.SUCCESS;
							else return ActionResultType.PASS;
						}
						//the chests are facing in the same direction, but are placed behind each other. the case where they are standing next to each other facing the same direction is covered before
						else if(facing == otherFacing)
						{
							if(connectChests(world, pos, otherPos, chestState, otherState, ctx.getFace(), facing.rotateY(), ctx.func_221532_j().x % 1, ctx.func_221532_j().z % 1, false))
								return ActionResultType.SUCCESS;
							else return ActionResultType.PASS;
						}
					}
				}

				return ActionResultType.PASS;
			}

			/**
			 * Connects two chests with each other based on hit data
			 * @param world The world the two chests are in
			 * @param clickedPos The position of the clicked chest
			 * @param otherPos The position of the chest to connect the clicked chest to
			 * @param clickedState The state of the clicked chest
			 * @param otherState The state of the chest to connect the clicked chest to
			 * @param clickedFace The face that got clicked
			 * @param chestFacing The facing of the chest that should be used to determine the new direction to face, can be arbitrary under certain circumstances.
			 * @param hitX The x coordinate that was hit on the face
			 * @param hitZ The z coordinate that was hit on the face
			 * @param swapDirections Whether to swap the directions to check the hit data on
			 * @return true if the chests were connected, false otherwise
			 */
			private boolean connectChests(World world, BlockPos clickedPos, BlockPos otherPos, BlockState clickedState, BlockState otherState, Direction clickedFace, Direction chestFacing, double hitX, double hitZ, boolean swapDirections)
			{
				Direction newFacing = Direction.UP;

				if(!swapDirections)
				{
					if(chestFacing == Direction.NORTH || chestFacing == Direction.SOUTH)
						newFacing = hitZ < 0.5 ? Direction.NORTH : Direction.SOUTH;
					else if(chestFacing == Direction.WEST || chestFacing == Direction.EAST)
						newFacing = hitX < 0.5 ? Direction.WEST : Direction.EAST;
				}
				else
				{
					if(chestFacing == Direction.WEST || chestFacing == Direction.EAST)
						newFacing = hitZ < 0.5 ? Direction.NORTH : Direction.SOUTH;
					else if(chestFacing == Direction.NORTH || chestFacing == Direction.SOUTH)
						newFacing = hitX < 0.5 ? Direction.WEST : Direction.EAST;
				}

				if(newFacing != Direction.UP)
				{
					ChestType newType = getNewChestType(clickedFace, newFacing);

					world.setBlockState(clickedPos, clickedState.with(ChestBlock.FACING, newFacing).with(ChestBlock.TYPE, newType));
					world.setBlockState(otherPos, otherState.with(ChestBlock.FACING, newFacing).with(ChestBlock.TYPE, newType.opposite()));
					return true;
				}

				return false;
			}

			/**
			 * Figures out which side the new connection should go on
			 * @param clickedFace The face that was clicked with the chest connector
			 * @param chestFacing The facing of the chest that was clicked
			 * @return The new chest type for the clicked chest
			 */
			private ChestType getNewChestType(Direction clickedFace, Direction chestFacing)
			{
				switch(chestFacing) //figure out whether the clicked chest will connect to the left or right
				{
					case NORTH: return clickedFace == Direction.WEST ? ChestType.RIGHT : ChestType.LEFT;
					case SOUTH: return clickedFace == Direction.WEST ? ChestType.LEFT : ChestType.RIGHT;
					case EAST: return clickedFace == Direction.NORTH ? ChestType.RIGHT : ChestType.LEFT;
					case WEST: return clickedFace == Direction.NORTH ? ChestType.LEFT : ChestType.RIGHT;
					default: return ChestType.SINGLE;
				}
			}
		}.setRegistryName(new ResourceLocation(MODID, "chest_wrench")));
	}
}
