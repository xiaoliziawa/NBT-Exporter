package com.lirxowo.nbtexporter;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class NBTLang {

    public static LangBuilder builder() {
        return new LangBuilder(NBTExporter.MODID);
    }

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().add(Component.translatable(String.format("%s.%s", NBTExporter.MODID, langKey), LangBuilder.resolveBuilders(args)));
    }

    public static MutableComponent translateDirect(String langKey, Object... args) {
        return Component.translatable(String.format("%s.%s", NBTExporter.MODID, langKey), LangBuilder.resolveBuilders(args));
    }
}
