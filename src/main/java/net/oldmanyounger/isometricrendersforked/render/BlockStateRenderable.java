package net.oldmanyounger.isometricrendersforked.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oldmanyounger.isometricrendersforked.IsometricRendersForked;
import net.oldmanyounger.isometricrendersforked.property.DefaultPropertyBundle;
import net.oldmanyounger.isometricrendersforked.util.ExportPathSpec;
import org.jetbrains.annotations.Nullable;

/**
 * Renderable wrapper for a block state.
 *
 * <p>This renders ordinary block models through Minecraft's block renderer and
 * supports optional block entity NBT when the rendered block provides a block
 * entity.</p>
 */
public class BlockStateRenderable extends DefaultRenderable<DefaultPropertyBundle> {
    private final BlockState state;
    private final @Nullable BlockEntity blockEntity;

    /**
     * Creates a block renderable.
     *
     * @param state the block state to render
     * @param blockEntity optional block entity for animated/special block renderers
     */
    public BlockStateRenderable(BlockState state, @Nullable BlockEntity blockEntity) {
        this.state = state;
        this.blockEntity = blockEntity;
    }

    // Creates a renderable from a block state.
    public static BlockStateRenderable of(BlockState state) {
        return of(state, null);
    }

    // Creates a renderable from a block state and optional block entity NBT.
    public static BlockStateRenderable of(BlockState state, @Nullable CompoundTag blockEntityNbt) {
        BlockEntity blockEntity = createBlockEntity(state, blockEntityNbt);
        return new BlockStateRenderable(state, blockEntity);
    }

    // Creates a renderable by copying state and block entity NBT from the world.
    public static BlockStateRenderable copyOf(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        CompoundTag blockEntityNbt = blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess());

        return of(state, blockEntityNbt);
    }

    // Creates and optionally populates a block entity for a block state.
    private static @Nullable BlockEntity createBlockEntity(BlockState state, @Nullable CompoundTag blockEntityNbt) {
        if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
            return null;
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, state);
        var level = Minecraft.getInstance().level;

        if (blockEntity == null || level == null) {
            return blockEntity;
        }

        blockEntity.setLevel(level);

        if (blockEntityNbt != null) {
            loadBlockEntityNbt(blockEntity, blockEntityNbt, level);
        }

        return blockEntity;
    }

    // Loads copied or command-provided NBT into a block entity at the render origin.
    private static void loadBlockEntityNbt(BlockEntity blockEntity, CompoundTag blockEntityNbt, Level level) {
        CompoundTag nbt = blockEntityNbt.copy();

        // Force origin coordinates so world-copied block entities render in local space.
        nbt.putInt("x", 0);
        nbt.putInt("y", 0);
        nbt.putInt("z", 0);

        try {
            blockEntity.loadWithComponents(nbt, level.registryAccess());
        } catch (RuntimeException exception) {
            IsometricRendersForked.LOGGER.warn("Failed to load block entity NBT for {}", blockEntity.getType(), exception);
        }
    }

    // Emits this block's model and optional block entity vertices.
    @Override
    public void emitVertices(PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta) {
        var minecraft = Minecraft.getInstance();

        poseStack.pushPose();
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        minecraft.getBlockRenderer().renderSingleBlock(
                this.state,
                poseStack,
                bufferSource,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
        );

        if (this.blockEntity != null) {
            minecraft.getBlockEntityRenderDispatcher().renderItem(
                    this.blockEntity,
                    poseStack,
                    bufferSource,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY
            );
        }

        poseStack.popPose();
    }

    // Returns the shared default transform properties.
    @Override
    public DefaultPropertyBundle properties() {
        return DefaultPropertyBundle.get();
    }

    // Builds the default export path for this block.
    @Override
    public ExportPathSpec exportPath() {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(this.state.getBlock());

        if (blockId == null) {
            blockId = ResourceLocation.fromNamespaceAndPath("unknown", "block");
        }

        return ExportPathSpec.ofIdentified(blockId, "block");
    }

    // Returns the rendered block state.
    public BlockState state() {
        return this.state;
    }
}
