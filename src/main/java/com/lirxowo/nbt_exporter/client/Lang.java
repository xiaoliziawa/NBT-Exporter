package com.lirxowo.nbt_exporter.client;

import com.lirxowo.nbt_exporter.Nbt_exporter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Lang {
    public static MutableComponent translate(String langKey, Object... args) {
        return Component.translatable(Nbt_exporter.MODID + "." + langKey, args);
    }
}
