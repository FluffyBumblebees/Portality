/*
 * This file is part of Worldgen Indicators.
 *
 * Copyright 2018, Buuz135
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.buuz135.portality.tile;

import com.buuz135.portality.block.BlockController;
import com.buuz135.portality.block.module.IPortalModule;
import com.buuz135.portality.data.PortalDataManager;
import com.buuz135.portality.data.PortalInformation;
import com.buuz135.portality.data.PortalLinkData;
import com.buuz135.portality.gui.GuiController;
import com.buuz135.portality.gui.button.PortalSettingButton;
import com.buuz135.portality.handler.ChunkLoaderHandler;
import com.buuz135.portality.handler.StructureHandler;
import com.buuz135.portality.handler.TeleportHandler;
import com.buuz135.portality.proxy.CommonProxy;
import com.buuz135.portality.proxy.PortalityConfig;
import com.buuz135.portality.proxy.PortalitySoundHandler;
import com.buuz135.portality.proxy.client.TickeableSound;
import com.hrznstudio.titanium.block.tile.TilePowered;
import com.hrznstudio.titanium.client.gui.addon.StateButtonInfo;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

public class TileController extends TilePowered {

    private static String NBT_FORMED = "Formed";
    private static String NBT_LENGTH = "Length";
    private static String NBT_WIDTH = "Width";
    private static String NBT_HEIGHT = "Height";
    private static String NBT_PORTAL = "Portal";
    private static String NBT_LINK = "Link";
    private static String NBT_DISPLAY = "Display";
    private static String NBT_ONCE = "Once";

    private boolean isFormed;
    private boolean onceCall;
    private boolean display;
    private PortalInformation information;
    private PortalLinkData linkData;

    @OnlyIn(Dist.CLIENT)
    private TickeableSound sound;

    private TeleportHandler teleportHandler;
    private StructureHandler structureHandler;

    public TileController() {
        super(CommonProxy.BLOCK_CONTROLLER);
        this.isFormed = false;
        this.onceCall = false;
        this.display = true;
        this.teleportHandler = new TeleportHandler(this);
        this.structureHandler = new StructureHandler(this);

        this.addButton(new PortalSettingButton(-22, 12, new StateButtonInfo(0, PortalSettingButton.RENAME)) {
            @Override
            public int getState() {
                return 0;
            }
        });

        this.addButton(new PortalSettingButton(-22, 12 + 22, new StateButtonInfo(0, PortalSettingButton.PUBLIC), new StateButtonInfo(1, PortalSettingButton.PRIVATE)) {
            @Override
            public int getState() {
                return information != null && information.isPrivate() ? 1 : 0;
            }
        }.setPredicate(nbtTagCompound -> togglePrivacy()));

        this.addButton(new PortalSettingButton(-22, 12 + 22 * 2, new StateButtonInfo(0, PortalSettingButton.NAME_SHOWN), new StateButtonInfo(1, PortalSettingButton.NAME_HIDDEN)) {
            @Override
            public int getState() {
                return display ? 0 : 1;
            }
        }.setPredicate(nbtTagCompound -> setDisplayNameEnabled(!display)));
    }

    @Override
    public void tick() {
        if (isActive()) {
            teleportHandler.tick();
            if (linkData != null) {
                for (Entity entity : this.world.getEntitiesWithinAABB(Entity.class, getPortalArea())) {
                    teleportHandler.addEntityToTeleport(entity, linkData);
                }
            }
        }
        if (!world.isRemote) {
            workModules();
        }
        if (world.isRemote) {
            tickSound();
            return;
        }
        if (isActive() && linkData != null) {
            this.getEnergyStorage().extractEnergy((linkData.isCaller() ? 2 : 1) * structureHandler.getLength() * PortalityConfig.COMMON.POWER_PORTAL_TICK.get(), false);
            if (this.getEnergyStorage().getEnergyStored() == 0 || !isFormed) {
                closeLink();
            }
        }
        if (this.world.getGameTime() % 10 == 0) {
            if (structureHandler.shouldCheckForStructure()) {
                this.isFormed = structureHandler.checkArea();
                if (this.isFormed) {
                    structureHandler.setShouldCheckForStructure(false);
                } else {
                    structureHandler.cancelFrameBlocks();
                }
            }
            if (isFormed) {
                getPortalInfo();
                if (linkData != null) {
                    ChunkLoaderHandler.addPortalAsChunkloader(this);
                    TileEntity tileEntity = this.world.getServer().getWorld(DimensionType.byName(linkData.getDimension())).getTileEntity(linkData.getPos());
                    if (!(tileEntity instanceof TileController) || ((TileController) tileEntity).getLinkData() == null || !((TileController) tileEntity).getLinkData().getDimension().equals(this.world.getDimension().getType().getRegistryName()) || !((TileController) tileEntity).getLinkData().getPos().equals(this.pos)) {
                        this.closeLink();
                    }
                }
            }
            markForUpdate();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tickSound() {
        if (isActive()) {
            if (sound == null) {
                Minecraft.getInstance().getSoundHandler().play(sound = new TickeableSound(this.world, this.pos, PortalitySoundHandler.PORTAL));
            } else {
                sound.increase();
            }
        } else if (sound != null) {
            if (sound.getPitch() > 0) {
                sound.decrease();
            } else {
                sound.setDone();
                sound = null;
            }
        }
    }

    @Override
    public NBTTagCompound write(NBTTagCompound compound) {
        compound = super.write(compound);
        compound.setBoolean(NBT_FORMED, isFormed);
        compound.setInt(NBT_LENGTH, structureHandler.getLength());
        compound.setInt(NBT_WIDTH, structureHandler.getWidth());
        compound.setInt(NBT_HEIGHT, structureHandler.getHeight());
        compound.setBoolean(NBT_ONCE, onceCall);
        compound.setBoolean(NBT_DISPLAY, display);
        if (information != null) compound.setTag(NBT_PORTAL, information.writetoNBT());
        if (linkData != null) compound.setTag(NBT_LINK, linkData.writeToNBT());
        return compound;
    }

    @Override
    public void read(NBTTagCompound compound) {
        isFormed = compound.getBoolean(NBT_FORMED);
        structureHandler.setLength(compound.getInt(NBT_LENGTH));
        structureHandler.setWidth(compound.getInt(NBT_WIDTH));
        structureHandler.setHeight(compound.getInt(NBT_HEIGHT));
        if (compound.hasKey(NBT_PORTAL))
            information = PortalInformation.readFromNBT(compound.getCompound(NBT_PORTAL));
        if (compound.hasKey(NBT_LINK))
            linkData = PortalLinkData.readFromNBT(compound.getCompound(NBT_LINK));
        onceCall = compound.getBoolean(NBT_ONCE);
        display = compound.getBoolean(NBT_DISPLAY);
        super.read(compound);
    }

    public void breakController() {
        closeLink();
        structureHandler.cancelFrameBlocks();
    }

    private void workModules() {
        boolean interdimensional = false;
        for (BlockPos pos : structureHandler.getModules()) {
            Block block = this.world.getBlockState(pos).getBlock();
            if (block instanceof IPortalModule) {
                if (((IPortalModule) block).allowsInterdimensionalTravel()) interdimensional = true;
                if (isActive()) ((IPortalModule) block).work(this, pos);
            }
        }
        PortalDataManager.setPortalInterdimensional(this.world, this.pos, interdimensional);
    }

    public AxisAlignedBB getPortalArea() {
        if (!(world.getBlockState(this.pos).getBlock() instanceof BlockController))
            return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
        EnumFacing facing = world.getBlockState(this.pos).get(BlockController.FACING);
        BlockPos corner1 = this.pos.offset(facing.rotateY(), structureHandler.getWidth() - 1).offset(EnumFacing.UP);
        BlockPos corner2 = this.pos.offset(facing.rotateY(), -structureHandler.getWidth() + 1).offset(EnumFacing.UP, structureHandler.getHeight() - 1).offset(facing.getOpposite(), structureHandler.getLength() - 1);
        return new AxisAlignedBB(corner1, corner2);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getPortalArea();
    }

    private void getPortalInfo() {
        information = PortalDataManager.getInfoFromPos(this.world, this.pos);
    }

    public void togglePrivacy() {
        PortalDataManager.setPortalPrivacy(this.world, this.pos, !information.isPrivate());
        getPortalInfo();
        markForUpdate();
    }

    public boolean isFormed() {
        return isFormed;
    }

    public boolean isPrivate() {
        return information != null && information.isPrivate();
    }

    public UUID getOwner() {
        if (information != null) return information.getOwner();
        return null;
    }

    public UUID getID() {
        if (information != null) return information.getId();
        return null;
    }

    public String getPortalDisplayName() {
        if (information != null) return information.getName();
        return "";
    }

    public void setDisplayName(String name) {
        if (information != null) information.setName(name);
    }

    public boolean isInterdimensional() {
        return information != null && information.isInterdimensional();
    }

    public ItemStack getDisplay() {
        if (information != null) return information.getDisplay();
        return ItemStack.EMPTY;
    }

    public void linkTo(PortalLinkData data, PortalLinkData.PortalCallType type) {
        if (type == PortalLinkData.PortalCallType.FORCE) {
            closeLink();
        }
        if (linkData != null) return;
        if (type == PortalLinkData.PortalCallType.SINGLE) onceCall = true;
        if (data.isCaller()) {
            World world = this.world.getServer().getWorld(DimensionType.byName(linkData.getDimension()));
            TileEntity entity = world.getTileEntity(data.getPos());
            if (entity instanceof TileController) {
                data.setName(((TileController) entity).getPortalDisplayName());
                ((TileController) entity).linkTo(new PortalLinkData(this.world.getDimension().getType().getRegistryName(), this.pos, false, this.getPortalDisplayName()), type);
                int power = PortalityConfig.COMMON.PORTAL_POWER_OPEN_INTERDIMENSIONAL.get();
                if (entity.getWorld().equals(this.world)) {
                    power = (int) this.pos.getDistance(entity.getPos().getX(), entity.getPos().getZ(), entity.getPos().getY()) * structureHandler.getLength();
                }
                this.getEnergyStorage().extractEnergy(power, false);
            }
        }
        PortalDataManager.setActiveStatus(this.world, this.pos, true);
        this.linkData = data;
    }

    public void closeLink() {
        if (linkData != null) {
            PortalDataManager.setActiveStatus(this.world, this.pos, false);
            World world = this.world.getServer().getWorld(DimensionType.byName(linkData.getDimension()));
            linkData = null;
            TileEntity entity = world.getTileEntity(linkData.getPos());
            if (entity instanceof TileController) {
                ((TileController) entity).closeLink();
            }
        }
        ChunkLoaderHandler.removePortalAsChunkloader(this);
    }

    public boolean isActive() {
        return information != null && information.isActive();
    }

    public PortalLinkData getLinkData() {
        return linkData;
    }

    public boolean isDisplayNameEnabled() {
        return display;
    }

    public void setDisplayNameEnabled(ItemStack display) {
        PortalDataManager.setPortalDisplay(this.world, this.pos, display);
        getPortalInfo();
        markForUpdate();
    }

    public void setDisplayNameEnabled(boolean display) {
        this.display = display;
        markForUpdate();
    }

    public List<BlockPos> getModules() {
        return structureHandler.getModules();
    }

    public boolean teleportedEntity() {
        if (onceCall) {
            onceCall = false;
            closeLink();
            return true;
        }
        return false;
    }

    public boolean isShouldCheckForStructure() {
        return structureHandler.shouldCheckForStructure();
    }

    public void setShouldCheckForStructure(boolean shouldCheckForStructure) {
        structureHandler.setShouldCheckForStructure(shouldCheckForStructure);
    }

    public int getWidth() {
        return structureHandler.getWidth();
    }

    public int getHeight() {
        return structureHandler.getHeight();
    }

    public int getLength() {
        return structureHandler.getLength();
    }

    @Override
    public boolean onActivated(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!super.onActivated(playerIn, hand, facing, hitX, hitY, hitZ)) {
            if (!world.isRemote()) {
                Minecraft.getInstance().addScheduledTask(() -> {
                    Minecraft.getInstance().displayGuiScreen(new GuiController(this));
                });
                return true;
            }

        }
        return false;
    }

}
