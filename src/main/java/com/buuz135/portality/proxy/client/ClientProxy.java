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
package com.buuz135.portality.proxy.client;

import com.buuz135.portality.proxy.CommonProxy;
import com.buuz135.portality.proxy.client.render.TESRPortal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class ClientProxy extends CommonProxy {

    @Override
    public void onClient(Minecraft instance) {
        super.onClient(instance);
        ClientRegistry.bindTileEntityRenderer(CommonProxy.BLOCK_CONTROLLER.getTileEntityType(), TESRPortal::new);
        ItemBlockRenderTypes.setRenderLayer(CommonProxy.BLOCK_CONTROLLER, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(CommonProxy.BLOCK_FRAME, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(CommonProxy.BLOCK_CAPABILITY_ENERGY_MODULE, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(CommonProxy.BLOCK_CAPABILITY_FLUID_MODULE, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(CommonProxy.BLOCK_INTERDIMENSIONAL_MODULE, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(CommonProxy.BLOCK_CAPABILITY_ITEM_MODULE, RenderType.cutout());
        Minecraft.getInstance().getBlockColors().register((state, world, pos, index) -> {
            if (index == 0 && world != null) {
                BlockEntity tileEntity = world.getBlockEntity(pos);
                if (tileEntity instanceof IPortalColor) {
                    return ((IPortalColor) tileEntity).getColor();
                }
            }
            return -16739073;
        }, CommonProxy.BLOCK_FRAME, CommonProxy.BLOCK_CONTROLLER, CommonProxy.BLOCK_CAPABILITY_ENERGY_MODULE, CommonProxy.BLOCK_CAPABILITY_FLUID_MODULE, CommonProxy.BLOCK_CAPABILITY_ITEM_MODULE, CommonProxy.BLOCK_INTERDIMENSIONAL_MODULE);
    }
}
