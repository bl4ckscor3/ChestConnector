package bl4ckscor3.mod.wrenchest;

import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.state.properties.ChestType;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
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
			public EnumActionResult onItemUseFirst(ItemStack stack, ItemUseContext ctx)
			{
				EnumActionResult result = checkConnections(stack, ctx);

				if(result == EnumActionResult.SUCCESS && !ctx.getPlayer().isCreative())
					stack.damageItem(1, ctx.getPlayer());

				return result;
			}

			/**
			 * Checks which way two chests might be facing each other and then connects them
			 * @see Item#onItemUseFirst
			 */
			private EnumActionResult checkConnections(ItemStack stack, ItemUseContext ctx)
			{
				if(!(ctx.getWorld().getBlockState(ctx.getPos()).getBlock() instanceof BlockChest))
					return EnumActionResult.PASS;

				World world = ctx.getWorld();
				BlockPos pos = ctx.getPos();
				IBlockState chestState = world.getBlockState(pos);

				if(chestState.get(BlockChest.TYPE) != ChestType.SINGLE) //disconnect double chests
				{
					EnumFacing facingTowardsOther = BlockChest.getDirectionToAttached(chestState);

					world.setBlockState(pos, chestState.with(BlockChest.TYPE, ChestType.SINGLE));
					world.setBlockState(pos.offset(facingTowardsOther), world.getBlockState(pos.offset(facingTowardsOther)).with(BlockChest.TYPE, ChestType.SINGLE));
					return EnumActionResult.SUCCESS;
				}
				else if(ctx.getFace() != EnumFacing.UP && ctx.getFace() != EnumFacing.DOWN) //connect single chests, UP/DOWN check is here so double chests can be disconected by clicking on all faces
				{
					BlockPos otherPos = pos.offset(ctx.getFace());
					IBlockState otherState = world.getBlockState(otherPos);

					if(otherState.getBlock() instanceof BlockChest && otherState.get(BlockChest.TYPE) == ChestType.SINGLE)
					{
						EnumFacing facing = chestState.get(BlockChest.FACING);
						EnumFacing otherFacing = otherState.get(BlockChest.FACING);

						//the chests are facing (away from) each other
						if((ctx.getFace() == facing || ctx.getFace() == otherFacing) && facing.getOpposite() == otherFacing)
						{
							if(connectChests(world, pos, otherPos, chestState, otherState, ctx.getFace(), facing, ctx.getHitX(), ctx.getHitZ(), true))
								return EnumActionResult.SUCCESS;
							else return EnumActionResult.PASS;
						}
						//the clicked chest has the other chest to its left/right
						else if(ctx.getFace().rotateY() == facing || ctx.getFace().rotateYCCW() == facing)
						{
							if(connectChests(world, pos, otherPos, chestState, otherState, ctx.getFace(), facing, ctx.getHitX(), ctx.getHitZ(), false))
								return EnumActionResult.SUCCESS;
							else return EnumActionResult.PASS;
						}
						//the clicked chest has its neighbor to the front/back
						else if(ctx.getFace().rotateY() == otherFacing || ctx.getFace().rotateYCCW() == otherFacing)
						{
							if(connectChests(world, pos, otherPos, chestState, otherState, ctx.getFace(), otherFacing, ctx.getHitX(), ctx.getHitZ(), false))
								return EnumActionResult.SUCCESS;
							else return EnumActionResult.PASS;
						}
						//the chests are facing in the same direction, but are placed behind each other. the case where they are standing next to each other facing the same direction is covered before
						else if(facing == otherFacing)
						{
							if(connectChests(world, pos, otherPos, chestState, otherState, ctx.getFace(), facing.rotateY(), ctx.getHitX(), ctx.getHitZ(), false))
								return EnumActionResult.SUCCESS;
							else return EnumActionResult.PASS;
						}
					}
				}

				return EnumActionResult.PASS;
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
			private boolean connectChests(World world, BlockPos clickedPos, BlockPos otherPos, IBlockState clickedState, IBlockState otherState, EnumFacing clickedFace, EnumFacing chestFacing, float hitX, float hitZ, boolean swapDirections)
			{
				EnumFacing newFacing = EnumFacing.UP;

				if(!swapDirections)
				{
					if(chestFacing == EnumFacing.NORTH || chestFacing == EnumFacing.SOUTH)
						newFacing = hitZ < 0.5 ? EnumFacing.NORTH : EnumFacing.SOUTH;
					else if(chestFacing == EnumFacing.WEST || chestFacing == EnumFacing.EAST)
						newFacing = hitX < 0.5 ? EnumFacing.WEST : EnumFacing.EAST;
				}
				else
				{
					if(chestFacing == EnumFacing.WEST || chestFacing == EnumFacing.EAST)
						newFacing = hitZ < 0.5 ? EnumFacing.NORTH : EnumFacing.SOUTH;
					else if(chestFacing == EnumFacing.NORTH || chestFacing == EnumFacing.SOUTH)
						newFacing = hitX < 0.5 ? EnumFacing.WEST : EnumFacing.EAST;
				}

				if(newFacing != EnumFacing.UP)
				{
					ChestType newType = getNewChestType(clickedFace, newFacing);

					world.setBlockState(clickedPos, clickedState.with(BlockChest.FACING, newFacing).with(BlockChest.TYPE, newType));
					world.setBlockState(otherPos, otherState.with(BlockChest.FACING, newFacing).with(BlockChest.TYPE, newType.opposite()));
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
			private ChestType getNewChestType(EnumFacing clickedFace, EnumFacing chestFacing)
			{
				switch(chestFacing) //figure out whether the clicked chest will connect to the left or right
				{
					case NORTH: return clickedFace == EnumFacing.WEST ? ChestType.RIGHT : ChestType.LEFT;
					case SOUTH: return clickedFace == EnumFacing.WEST ? ChestType.LEFT : ChestType.RIGHT;
					case EAST: return clickedFace == EnumFacing.NORTH ? ChestType.RIGHT : ChestType.LEFT;
					case WEST: return clickedFace == EnumFacing.NORTH ? ChestType.LEFT : ChestType.RIGHT;
					default: return ChestType.SINGLE;
				}
			}
		}.setRegistryName(new ResourceLocation(MODID, "chest_wrench")));
	}
}
