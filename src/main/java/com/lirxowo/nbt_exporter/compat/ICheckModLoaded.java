package com.lirxowo.nbt_exporter.compat;

import net.minecraftforge.fml.ModList;

public interface ICheckModLoaded {
    static boolean hasMod(String modid) {
        return ModList.get().isLoaded(modid);
    }

    static boolean hasCreate() {
        return hasMod("create");
    }

    static boolean hasIE() {
        return hasMod("immersiveengineering");
    }

    static boolean hasMekanism() {
        return hasMod("mekanism");
    }
}
