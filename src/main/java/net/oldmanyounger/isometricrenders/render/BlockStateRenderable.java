package net.oldmanyounger.isometricrenders.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oldmanyounger.isometricrenders.property.DefaultPropertyBundle;
import net.oldmanyounger.isometricrenders.util.ExportPathSpec;
import org.jetbrains.annotations.Nullable;

/**
 * Renderable wrapper for a block state.
 *
 * <p>This renders ordinary block models through Minecraft's block renderer and
 * attempts to create a default block entity for blocks that provide one.</p>
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
        BlockEntity blockEntity = null;

        if (state.getBlock() instanceof EntityBlock entityBlock) {
            blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, state);

            if (blockEntity != null && Minecraft.getInstance().level != null) {
                blockEntity.setLevel(Minecraft.getInstance().level);
            }
        }

        return new BlockStateRenderable(state, blockEntity);
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
