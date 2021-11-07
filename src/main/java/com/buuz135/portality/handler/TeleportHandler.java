/**
 * MIT License
 *
 * Copyright (c) 2018
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.buuz135.portality.handler;

import com.buuz135.portality.Portality;
import com.buuz135.portality.block.ControllerBlock;
import com.buuz135.portality.data.PortalLinkData;
import com.buuz135.portality.network.PortalTeleportMessage;
import com.buuz135.portality.proxy.PortalityConfig;
import com.buuz135.portality.proxy.PortalitySoundHandler;
import com.buuz135.portality.tile.ControllerTile;
import com.hrznstudio.titanium.util.TeleportationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlaySoundPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;

import java.util.*;

public class TeleportHandler {

    private HashMap<Entity, TeleportData> entityTimeToTeleport;
    private HashMap<Entity, TeleportedEntityData> entitesTeleported;
    private ControllerTile controller;

    public TeleportHandler(ControllerTile controller) {
        entityTimeToTeleport = new HashMap<>();
        entitesTeleported = new HashMap<>();
        this.controller = controller;
    }

    public void addEntityToTeleport(Entity entity, PortalLinkData data) {
        if (!entityTimeToTeleport.containsKey(entity)) entityTimeToTeleport.put(entity, new TeleportData(data));
    }

    public void tick() {
        if (!(controller.getWorld().getBlockState(controller.getPos()).getBlock() instanceof ControllerBlock)) {
            controller.closeLink();
            return;
        }
        Direction facing = controller.getWorld().getBlockState(controller.getPos()).get(ControllerBlock.FACING_HORIZONTAL).getOpposite();
        Random random = controller.getWorld().rand;
        BlockPos offset = controller.getPos().offset(facing);
        double mult = controller.getLength() / 20D;
        controller.getWorld().addParticle(ParticleTypes.END_ROD, offset.getX() + 0.5 + random.nextDouble() * (controller.getWidth() + 2) - (controller.getWidth() + 2) / 2D, offset.getY() + controller.getHeight() / 2D + random.nextDouble() * (controller.getHeight() - 2) - (controller.getHeight() - 2) / 2D, offset.getZ() + 0.5 + random.nextDouble() * 2 - 1, facing.getDirectionVec().getX() * mult, facing.getDirectionVec().getY() * mult, facing.getDirectionVec().getZ() * mult);
        List<Entity> entityRemove = new ArrayList<>();
        for (Map.Entry<Entity, TeleportData> entry : entityTimeToTeleport.entrySet()) {
            if (!entry.getKey().isAlive() || !controller.getWorld().getEntitiesWithinAABB(Entity.class, controller.getPortalArea()).contains(entry.getKey())) {
                entityRemove.add(entry.getKey());
                continue;
            }
            if (entry.getKey() instanceof PlayerEntity && entry.getKey().isCrouching()) {
                entityRemove.add(entry.getKey());
                continue;
            }
            BlockPos destinationPos = controller.getPos().add(0.5, controller.getHeight() / 2D - 0.75, 0.5).offset(facing, controller.getLength() - 1);
            Vector3d destination = new Vector3d(destinationPos.getX(), destinationPos.getY(), destinationPos.getZ()).add(0.5, 0, 0.5);
            double distance = destinationPos.manhattanDistance(new Vector3i(entry.getKey().getPosition().getX(), entry.getKey().getPosition().getY(), entry.getKey().getPosition().getZ()));
            destination = destination.subtract(entry.getKey().getPosition().getX(), entry.getKey().getPosition().getY(), entry.getKey().getPosition().getZ()).scale((entry.getValue().time += 0.05) / distance);
            if (destinationPos.withinDistance(new Vector3i(entry.getKey().getPosition().getX(), entry.getKey().getPosition().getY(), entry.getKey().getPosition().getZ()), 1.5)) {
                if (!entry.getKey().world.isRemote) {
                    if (controller.getEnergyStorage().getEnergyStored() >= PortalityConfig.TELEPORT_ENERGY_AMOUNT) {
                        World tpWorld = entry.getKey().world.getServer().getWorld(entry.getValue().data.getDimension());
                        Direction tpFacing = Direction.NORTH;
                        if (controller.getLinkData().isToken()){
                            tpFacing = Direction.byName(controller.getTeleportationTokens().get(controller.getLinkData().getName()).getString("Direction"));
                        } else {
                            tpFacing = tpWorld.getBlockState(entry.getValue().data.getPos()).get(ControllerBlock.FACING_HORIZONTAL);
                        }
                        BlockPos pos = entry.getValue().data.getPos().offset(tpFacing, 2);
                        Entity entity = TeleportationUtils.teleportEntity(entry.getKey(), entry.getValue().data.getDimension(), pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, tpFacing.getHorizontalAngle(), 0);
                        entitesTeleported.put(entity, new TeleportedEntityData(entry.getValue().data));
                        controller.getEnergyStorage().extractEnergy(PortalityConfig.TELEPORT_ENERGY_AMOUNT, false);
                        if (entry.getKey() instanceof ServerPlayerEntity)
                            Portality.NETWORK.get().sendTo(new PortalTeleportMessage(tpFacing.getIndex(), controller.getLength()), ((ServerPlayerEntity) entry.getKey()).connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
                        if (controller.teleportedEntity()) {
                            return;
                        }
                    } else {
                        if (entry.getKey() instanceof LivingEntity && PortalityConfig.HURT_PLAYERS) {
                            ((LivingEntity) entry.getKey()).addPotionEffect(new EffectInstance(Effects.WITHER, 5 * 20, 1));
                        }
                    }
                }
                entityRemove.add(entry.getKey());
                continue;
            }
            entry.getKey().setMotion(destination.x, destination.y, destination.z);
        }
        for (Entity entity : entityRemove) {
            entityTimeToTeleport.remove(entity);
        }
        entityRemove.clear();
        for (Map.Entry<Entity, TeleportedEntityData> entry : entitesTeleported.entrySet()) {
            entry.getValue().ticks++;
            if (entry.getValue().ticks > 2 && !entry.getValue().moved) {
                if (entry.getKey().world.isRemote)
                    entry.getKey().world.getEntitiesWithinAABB(ServerPlayerEntity.class, new AxisAlignedBB(entry.getKey().getPosition().getX(), entry.getKey().getPosition().getY(), entry.getKey().getPosition().getZ(), entry.getKey().getPosition().getX(), entry.getKey().getPosition().getY(), entry.getKey().getPosition().getZ()).grow(16)).forEach(entityPlayer -> entityPlayer.connection.sendPacket(new SPlaySoundPacket(PortalitySoundHandler.PORTAL_TP.getRegistryName(), SoundCategory.BLOCKS, new Vector3d(entry.getKey().getPosition().getX(), entry.getKey().getPosition().getY(), entry.getKey().getPosition().getZ()), 0.5f, 1f)));
                entry.getValue().moved = true;
                World tpWorld = entry.getKey().world;
                Direction tpFacing = Direction.NORTH;
                if (controller.getLinkData().isToken()){
                    tpFacing = Direction.byName(controller.getTeleportationTokens().get(controller.getLinkData().getName()).getString("Direction"));
                } else if (tpWorld.getBlockState(entry.getValue().data.getPos()).getBlock() instanceof ControllerBlock){
                    tpFacing = tpWorld.getBlockState(entry.getValue().data.getPos()).get(ControllerBlock.FACING_HORIZONTAL);
                }
                Vector3d vec3d = new Vector3d(tpFacing.getDirectionVec().getX(), tpFacing.getDirectionVec().getY(), tpFacing.getDirectionVec().getZ()).scale(2 * controller.getLength() / (double) PortalityConfig.MAX_PORTAL_LENGTH);
                entry.getKey().setMotion(vec3d.x, vec3d.y, vec3d.z);
                entry.getKey().setRotationYawHead(tpFacing.getHorizontalAngle());
            }
            if (entry.getValue().ticks > 40) {
                entityRemove.add(entry.getKey());
            }
        }
        for (Entity entity : entityRemove) {
            entitesTeleported.remove(entity);
        }
    }

    private class TeleportData {
        private PortalLinkData data;
        private double time;

        public TeleportData(PortalLinkData data) {
            this.data = data;
            this.time = 0;
        }
    }

    private class TeleportedEntityData {

        private int ticks;
        private boolean moved;
        private PortalLinkData data;

        public TeleportedEntityData(PortalLinkData data) {
            this.data = data;
            this.ticks = 0;
            this.moved = false;
        }
    }
}
