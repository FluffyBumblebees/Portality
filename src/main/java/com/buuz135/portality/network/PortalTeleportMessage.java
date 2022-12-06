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
package com.buuz135.portality.network;

import com.buuz135.portality.proxy.PortalityConfig;
import com.buuz135.portality.proxy.PortalitySoundHandler;
import com.hrznstudio.titanium.network.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class PortalTeleportMessage extends Message {

    private int facing;
    private int length;

    public PortalTeleportMessage(int facing, int length) {
        this.facing = facing;
        this.length = length;
    }

    public PortalTeleportMessage() {

    }

    @Override
    protected void handleServer() {
        Minecraft.getInstance().submitAsync(() -> {
            //Minecraft.getMinecraft().player.playSound(new SoundEvent(new ResourceLocation("entity.shulker.teleport")), 1, 1);
            Minecraft.getInstance().player.playSound(PortalitySoundHandler.PORTAL_TP, 0.1f, 1f);
            if (PortalityConfig.INSTANCE.LAUNCH_PLAYERS) {
                Direction facing = Direction.values()[this.facing];
                Vec3 vector = new Vec3(facing.getNormal().getX(), facing.getNormal().getY(), facing.getNormal().getZ()).scale(2 * length / (double) PortalityConfig.INSTANCE.MAX_PORTAL_LENGTH);
                LocalPlayer player = Minecraft.getInstance().player;
                player.setDeltaMovement(vector.x, vector.y, vector.z);
            }
        });
    }

}
