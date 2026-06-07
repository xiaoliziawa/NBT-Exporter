package com.lirxowo.nbt_exporter.command;

import com.lirxowo.nbt_exporter.Nbt_exporter;
import com.lirxowo.nbt_exporter.client.exporter.StructureExportScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = Nbt_exporter.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ExporterCommand {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        Minecraft minecraft = Minecraft.getInstance();

        dispatcher.register(Commands.literal("nbtexport")
                .executes((context) -> {
                    minecraft.tell(() -> minecraft.setScreen(new StructureExportScreen()));
                    return 1;
                })
                .then(Commands.argument("file", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            Path schematicsDir = minecraft.gameDirectory.toPath().resolve("schematics");
                            if (Files.isDirectory(schematicsDir)) {
                                try (Stream<Path> stream = Files.list(schematicsDir)) {
                                    stream.filter((path) -> path.toString().endsWith(".nbt"))
                                            .map((path) -> path.getFileName().toString())
                                            .filter((name) -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                            .forEach(builder::suggest);
                                } catch (Exception ignored) {
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes((context) -> {
                            String file = StringArgumentType.getString(context, "file");
                            minecraft.tell(() -> minecraft.setScreen(new StructureExportScreen(file)));
                            return 1;
                        })
                )
        );
    }
}
