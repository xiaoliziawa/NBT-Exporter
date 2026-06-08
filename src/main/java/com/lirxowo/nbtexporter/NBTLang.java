package com.lirxowo.nbtexporter;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class NBTLang {

    public static MutableComponent translateDirect(String langKey, Object... args) {
        return Component.translatable(String.format("%s.%s", NBTExporter.MODID, langKey), args);
    }
}
