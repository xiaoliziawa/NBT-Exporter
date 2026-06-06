package com.lirxowo.nbtexporter.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.lirxowo.nbtexporter.NBTExporter;
import com.lirxowo.nbtexporter.exporter.StructureExportScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@EventBusSubscriber(modid = NBTExporter.MODID, value = Dist.CLIENT)
public class ExporterCommand {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        Minecraft minecraft = Minecraft.getInstance();
        dispatcher.register(Commands.literal("nbt_exporter").then(Commands.literal("export").executes(context -> {
            minecraft.tell(() -> minecraft.setScreen(new StructureExportScreen()));
            return 1;
        }).then(Commands.argument("file", StringArgumentType.string()).suggests((context, builder) -> {
            Path schematicsDir = minecraft.gameDirectory.toPath().resolve("schematics");
            if (Files.isDirectory(schematicsDir)) {
                try (Stream<Path> stream = Files.list(schematicsDir)) {
                    stream.filter((path) -> {
                        return path.toString().endsWith(".nbt");
                    }).map((path) -> {
                        return path.getFileName().toString();
                    }).filter((name) -> {
                        return name.toLowerCase().startsWith(builder.getRemainingLowerCase());
                    }).forEach(builder::suggest);
                } catch (Exception ignored) {
                }
            }
            return builder.buildFuture();
        }).executes((context) -> {
            String file = StringArgumentType.getString(context, "file");
            minecraft.tell(() -> {
                minecraft.setScreen(new StructureExportScreen(file));
            });
            return 1;
        }))));
    }
}
