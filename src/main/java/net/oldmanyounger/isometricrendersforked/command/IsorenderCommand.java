package net.oldmanyounger.isometricrendersforked.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.oldmanyounger.isometricrendersforked.IsometricRendersForked;
import net.oldmanyounger.isometricrendersforked.render.AreaRenderable;
import net.oldmanyounger.isometricrendersforked.render.BlockStateRenderable;
import net.oldmanyounger.isometricrendersforked.render.EntityRenderable;
import net.oldmanyounger.isometricrendersforked.render.ItemRenderable;
import net.oldmanyounger.isometricrendersforked.render.Renderable;
import net.oldmanyounger.isometricrendersforked.render.TooltipRenderable;
import net.oldmanyounger.isometricrendersforked.screen.RenderScreen;
import net.oldmanyounger.isometricrendersforked.util.AreaSelectionHelper;
import net.oldmanyounger.isometricrendersforked.util.Translate;

/**
 * Client command entry point for Isometric Renders.
 *
 * <p>This command tree supports the current NeoForge baseline render targets:
 * items, blocks, entities, selected areas, and item tooltips.</p>
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
                                .executes(IsorenderCommand::openHeldItemRenderScreen)
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .executes(IsorenderCommand::openItemRenderScreen)))
                        .then(Commands.literal("tooltip")
                                .executes(IsorenderCommand::openHeldTooltipRenderScreen)
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .executes(IsorenderCommand::openTooltipRenderScreen)))
                        .then(Commands.literal("block")
                                .executes(IsorenderCommand::openTargetedBlockRenderScreen)
                                .then(Commands.argument("block", BlockStateArgument.block(event.getBuildContext()))
                                        .executes(IsorenderCommand::openBlockRenderScreen)
                                        .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                                .executes(IsorenderCommand::openBlockRenderScreenWithNbt))))
                        .then(Commands.literal("entity")
                                .executes(IsorenderCommand::openTargetedEntityRenderScreen)
                                .then(Commands.argument("entity", ResourceArgument.resource(event.getBuildContext(), Registries.ENTITY_TYPE))
                                        .executes(IsorenderCommand::openEntityRenderScreen)
                                        .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                                .executes(IsorenderCommand::openEntityRenderScreenWithNbt))))
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
                () -> Translate.msg("version", Component.literal(IsometricRendersForked.MOD_ID)),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    // Opens the held main-hand item in the temporary item render preview screen.
    private static int openHeldItemRenderScreen(CommandContext<CommandSourceStack> context) {
        ItemStack stack = heldStack();

        if (stack.isEmpty()) {
            context.getSource().sendFailure(Translate.msg("no_item"));
            return 0;
        }

        return openItemRenderable(context, stack);
    }

    // Parses an item argument and opens the temporary item render preview screen.
    private static int openItemRenderScreen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = ItemArgument.getItem(context, "item").createItemStack(1, false);
        return openItemRenderable(context, stack);
    }

    // Opens an item renderable and reports the default export path.
    private static int openItemRenderable(CommandContext<CommandSourceStack> context, ItemStack stack) {
        ItemRenderable renderable = new ItemRenderable(stack);
        openScreen(renderable);

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

    // Opens the held main-hand item's tooltip in the temporary preview screen.
    private static int openHeldTooltipRenderScreen(CommandContext<CommandSourceStack> context) {
        ItemStack stack = heldStack();

        if (stack.isEmpty()) {
            context.getSource().sendFailure(Translate.msg("no_item"));
            return 0;
        }

        return openTooltipRenderable(context, stack);
    }

    // Parses an item argument and opens the tooltip render preview screen.
    private static int openTooltipRenderScreen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = ItemArgument.getItem(context, "item").createItemStack(1, false);
        return openTooltipRenderable(context, stack);
    }

    // Opens a tooltip renderable and reports the default export path.
    private static int openTooltipRenderable(CommandContext<CommandSourceStack> context, ItemStack stack) {
        TooltipRenderable renderable = new TooltipRenderable(stack);
        openScreen(renderable);

        context.getSource().sendSuccess(
                () -> Translate.msg(
                        "tooltip_render_opened",
                        stack.getHoverName(),
                        Component.literal(renderable.exportPath().resolveFile("png").getPath())
                ),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    // Opens the currently targeted block and copies block entity NBT when present.
    private static int openTargetedBlockRenderScreen(CommandContext<CommandSourceStack> context) {
        var minecraft = Minecraft.getInstance();

        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult blockHitResult)) {
            context.getSource().sendFailure(Translate.msg("no_block"));
            return 0;
        }

        BlockPos pos = blockHitResult.getBlockPos();

        if (minecraft.level.getBlockState(pos).isAir()) {
            context.getSource().sendFailure(Translate.msg("no_block"));
            return 0;
        }

        BlockStateRenderable renderable = BlockStateRenderable.copyOf(minecraft.level, pos);
        return openBlockRenderable(context, renderable, Component.literal(renderable.state() + " at " + format(pos)));
    }

    // Parses a block state argument and opens the temporary block render preview screen.
    private static int openBlockRenderScreen(CommandContext<CommandSourceStack> context) {
        var blockInput = BlockStateArgument.getBlock(context, "block");
        var renderable = BlockStateRenderable.of(blockInput.getState());

        return openBlockRenderable(context, renderable, Component.literal(blockInput.getState().toString()));
    }

    // Parses a block state plus NBT and opens the temporary block render preview screen.
    private static int openBlockRenderScreenWithNbt(CommandContext<CommandSourceStack> context) {
        var blockInput = BlockStateArgument.getBlock(context, "block");
        CompoundTag nbt = CompoundTagArgument.getCompoundTag(context, "nbt");
        var renderable = BlockStateRenderable.of(blockInput.getState(), nbt);

        return openBlockRenderable(context, renderable, Component.literal(blockInput.getState().toString()));
    }

    // Opens a block renderable and reports the default export path.
    private static int openBlockRenderable(CommandContext<CommandSourceStack> context, BlockStateRenderable renderable, Component label) {
        openScreen(renderable);

        context.getSource().sendSuccess(
                () -> Translate.msg(
                        "block_render_opened",
                        label,
                        Component.literal(renderable.exportPath().resolveFile("png").getPath())
                ),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    // Opens the currently targeted entity and copies its NBT when possible.
    private static int openTargetedEntityRenderScreen(CommandContext<CommandSourceStack> context) {
        var minecraft = Minecraft.getInstance();

        if (!(minecraft.hitResult instanceof EntityHitResult entityHitResult)) {
            context.getSource().sendFailure(Translate.msg("no_entity"));
            return 0;
        }

        EntityRenderable renderable = EntityRenderable.copyOf(entityHitResult.getEntity());

        if (renderable == null) {
            context.getSource().sendFailure(Translate.msg("entity_render_failed", entityHitResult.getEntity().getDisplayName()));
            return 0;
        }

        return openEntityRenderable(context, renderable);
    }

    // Parses an entity type argument and opens the temporary entity render preview screen.
    private static int openEntityRenderScreen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityType<?> type = ResourceArgument.getSummonableEntityType(context, "entity").value();
        EntityRenderable renderable = EntityRenderable.of(type);

        return openEntityRenderable(context, renderable, type);
    }

    // Parses an entity type plus NBT and opens the temporary entity render preview screen.
    private static int openEntityRenderScreenWithNbt(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityType<?> type = ResourceArgument.getSummonableEntityType(context, "entity").value();
        CompoundTag nbt = CompoundTagArgument.getCompoundTag(context, "nbt");
        EntityRenderable renderable = EntityRenderable.of(type, nbt);

        return openEntityRenderable(context, renderable, type);
    }

    // Opens a typed entity renderable and handles entity creation failure.
    private static int openEntityRenderable(CommandContext<CommandSourceStack> context, EntityRenderable renderable, EntityType<?> type) {
        if (renderable == null) {
            context.getSource().sendFailure(Translate.msg("entity_render_failed", Component.literal(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString())));
            return 0;
        }

        return openEntityRenderable(context, renderable);
    }

    // Opens an entity renderable and reports the default export path.
    private static int openEntityRenderable(CommandContext<CommandSourceStack> context, EntityRenderable renderable) {
        openScreen(renderable);

        context.getSource().sendSuccess(
                () -> Translate.msg(
                        "entity_render_opened",
                        Component.literal(BuiltInRegistries.ENTITY_TYPE.getKey(renderable.entity().getType()).toString()),
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

        openScreen(renderable);

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

    // Opens a render screen on the client thread.
    private static void openScreen(Renderable<?> renderable) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new RenderScreen(renderable)));
    }

    // Returns a defensive copy of the current main-hand stack.
    private static ItemStack heldStack() {
        var player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : player.getMainHandItem().copy();
    }

    // Formats a block position for chat.
    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
