package net.oldmanyounger.isometricrenders.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.oldmanyounger.isometricrenders.IsometricRenders;
import net.oldmanyounger.isometricrenders.render.AreaRenderable;
import net.oldmanyounger.isometricrenders.render.BlockStateRenderable;
import net.oldmanyounger.isometricrenders.render.EntityRenderable;
import net.oldmanyounger.isometricrenders.render.ItemRenderable;
import net.oldmanyounger.isometricrenders.screen.RenderScreen;
import net.oldmanyounger.isometricrenders.util.AreaSelectionHelper;
import net.oldmanyounger.isometricrenders.util.Translate;

/**
 * Client command entry point for Isometric Renders.
 *
 * <p>This command tree is intentionally minimal while the full renderer and UI
 * are being ported.</p>
 */
public final class IsorenderCommand {
    private IsorenderCommand() {}

    // Registers the client-only /isorender command tree.
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("isorender")
                        .executes(IsorenderCommand::showStatus)
                        .then(Commands.literal("version")
                                .executes(IsorenderCommand::showVersion))
                        .then(Commands.literal("item")
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .executes(IsorenderCommand::openItemRenderScreen)))
                        .then(Commands.literal("block")
                                .then(Commands.argument("block", BlockStateArgument.block(event.getBuildContext()))
                                        .executes(IsorenderCommand::openBlockRenderScreen)))
                        .then(Commands.literal("entity")
                                .then(Commands.argument("entity", ResourceArgument.resource(event.getBuildContext(), Registries.ENTITY_TYPE))
                                        .executes(IsorenderCommand::openEntityRenderScreen)))
                        .then(Commands.literal("area")
                                .executes(IsorenderCommand::openSelectedAreaRenderScreen)
                                .then(Commands.argument("start", BlockPosArgument.blockPos())
                                        .then(Commands.argument("end", BlockPosArgument.blockPos())
                                                .executes(IsorenderCommand::openAreaRenderScreen))))

        );
    }

    // Shows a temporary status message for the port skeleton.
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Translate.msg("command_status"),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    // Shows the currently loaded port identity.
    private static int showVersion(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Translate.msg("version", Component.literal(IsometricRenders.MOD_ID)),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    // Parses an item argument and opens the temporary item render preview screen.
    private static int openItemRenderScreen(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ItemStack stack = ItemArgument.getItem(context, "item").createItemStack(1, false);
        ItemRenderable renderable = new ItemRenderable(stack);

        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new RenderScreen(renderable)));

        context.getSource().sendSuccess(
                () -> Translate.msg(
                        "item_render_opened",
                        stack.getHoverName(),
                        Component.literal(renderable.exportPath().resolveFile("png").getPath())
                ),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    // Parses a block state argument and opens the temporary block render preview screen.
    private static int openBlockRenderScreen(CommandContext<CommandSourceStack> context) {
        var blockInput = BlockStateArgument.getBlock(context, "block");
        var renderable = BlockStateRenderable.of(blockInput.getState());

        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new RenderScreen(renderable)));

        context.getSource().sendSuccess(
                () -> Translate.msg(
                        "block_render_opened",
                        Component.literal(blockInput.getState().toString()),
                        Component.literal(renderable.exportPath().resolveFile("png").getPath())
                ),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    // Parses an entity type argument and opens the temporary entity render preview screen.
    private static int openEntityRenderScreen(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        EntityType<?> type = ResourceArgument.getSummonableEntityType(context, "entity").value();
        EntityRenderable renderable = EntityRenderable.of(type);

        if (renderable == null) {
            context.getSource().sendFailure(Translate.msg("entity_render_failed", Component.literal(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString())));
            return 0;
        }

        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new RenderScreen(renderable)));

        context.getSource().sendSuccess(
                () -> Translate.msg(
                        "entity_render_opened",
                        Component.literal(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()),
                        Component.literal(renderable.exportPath().resolveFile("png").getPath())
                ),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    // Opens the current two-corner area selection.
    private static int openSelectedAreaRenderScreen(CommandContext<CommandSourceStack> context) {
        if (!AreaSelectionHelper.tryOpenScreen()) {
            context.getSource().sendFailure(Translate.msg("incomplete_selection"));
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Translate.msg("area_render_opened"),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    // Parses two block positions and opens an area render preview screen.
    private static int openAreaRenderScreen(CommandContext<CommandSourceStack> context) {
        BlockPos start = BlockPosArgument.getBlockPos(context, "start");
        BlockPos end = BlockPosArgument.getBlockPos(context, "end");
        AreaRenderable renderable = AreaRenderable.of(start, end);

        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new RenderScreen(renderable)));

        context.getSource().sendSuccess(
                () -> Translate.msg(
                        "area_render_opened_with_size",
                        renderable.blockCount(),
                        Component.literal(renderable.exportPath().resolveFile("png").getPath())
                ),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

}
