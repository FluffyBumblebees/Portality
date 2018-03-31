package com.buuz135.portality.block;

import com.buuz135.portality.data.PortalDataManager;
import com.buuz135.portality.data.PortalInformation;
import com.buuz135.portality.gui.GuiHandler;
import com.buuz135.portality.proxy.client.render.TESRPortal;
import com.buuz135.portality.tile.TileController;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import java.util.UUID;

public class BlockController extends BlockTileHorizontal<TileController> {

    public BlockController() {
        super("controller", TileController.class, Material.ROCK, GuiHandler.CONTROLLER);
    }

    @Override
    public void registerRender() {
        super.registerRender();
        ClientRegistry.bindTileEntitySpecialRenderer(TileController.class, new TESRPortal());
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        PortalInformation information = new PortalInformation(UUID.randomUUID(), placer.getUniqueID(), false, false, worldIn.provider.getDimension(), pos, "Dim: " + worldIn.provider.getDimension() + " X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ());
        PortalDataManager.addInformation(worldIn, information);
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    }

    @Override
    public void onBlockDestroyedByPlayer(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockDestroyedByPlayer(worldIn, pos, state);
        PortalDataManager.removeInformation(worldIn, pos);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile != null && tile instanceof TileController) {
            TileController controller = (TileController) tile;
            if (!controller.isFormed()) {
                playerIn.sendStatusMessage(new TextComponentTranslation(TextFormatting.RED + "ERROR: Portal needs to be 3 blocks deep!"), true);
                return true;
            }
            System.out.println(controller.getOwner());
            System.out.println(playerIn.getUniqueID());
            if (controller.isPrivate() && !controller.getOwner().equals(playerIn.getUniqueID())) {
                playerIn.sendStatusMessage(new TextComponentTranslation(TextFormatting.RED + "ERROR: This portal is private!"), true);
                return true;
            }
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }
}
