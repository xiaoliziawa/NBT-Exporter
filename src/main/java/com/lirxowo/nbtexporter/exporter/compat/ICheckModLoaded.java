package com.lirxowo.nbtexporter.exporter.compat;

import net.neoforged.fml.ModList;

public class ICheckModLoaded {

    @SuppressWarnings("unused")
    public static boolean hasIE() {
        return ModList.get().isLoaded("immersiveengineering");
    }

    public static boolean hasMekanism() {
        return ModList.get().isLoaded("mekanism");
    }
}
