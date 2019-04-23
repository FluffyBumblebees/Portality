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
package com.buuz135.portality;

import com.buuz135.portality.proxy.CommonProxy;
import com.buuz135.portality.proxy.PortalityConfig;
import com.buuz135.portality.proxy.client.ClientProxy;
import com.hrznstudio.titanium.util.TitaniumMod;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod("portality")
public class Portality extends TitaniumMod {

    public static final String MOD_ID = "portality";
    public static final String MOD_NAME = "Portality";
    public static final String VERSION = "1.0-SNAPSHOT";
    public static final ItemGroup TAB = new ItemGroup(MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(CommonProxy.BLOCK_CONTROLLER);
        }
    };

    public static CommonProxy proxy;

    public Portality() {
        proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PortalityConfig.BUILDER.build());
        addBlocks(CommonProxy.BLOCK_CONTROLLER,
                CommonProxy.BLOCK_FRAME,
                CommonProxy.BLOCK_CAPABILITY_ENERGY_MODULE,
                CommonProxy.BLOCK_CAPABILITY_FLUID_MODULE,
                CommonProxy.BLOCK_CAPABILITY_ITEM_MODULE_INPUT,
                CommonProxy.BLOCK_INTERDIMENSIONAL_MODULE);
    }

    @EventReceiver
    public void onCommon(FMLCommonSetupEvent event) {
        proxy.onCommon();
    }

    @EventReceiver
    public void onClient(FMLClientSetupEvent event) {
        proxy.onClient();
    }

}
