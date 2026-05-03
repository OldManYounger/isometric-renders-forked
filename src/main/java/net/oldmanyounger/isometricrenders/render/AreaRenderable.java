package net.oldmanyounger.isometricrenders.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oldmanyounger.isometricrenders.property.DefaultPropertyBundle;
import net.oldmanyounger.isometricrenders.util.ExportPathSpec;

/**
 * Renderable wrapper for a selected world area.
 *
 * <p>This NeoForge-first implementation renders blocks directly from the client
 * level instead of using worldmesher. It is suitable for small and medium
 * structures and can be replaced or optimized later if worldmesher support is
 * added.</p>
 */
public class AreaRenderable extends DefaultRenderable<AreaRenderable.AreaPropertyBundle> {
    private final BlockPos min;
    private final BlockPos max;
    private final int xSize;
    private final int ySize;
    private final int zSize;

    /**
     * Creates an area renderable from two inclusive corner positions.
     *
     * @param first first selected corner
     * @param second second selected corner
     */
    public AreaRenderable(BlockPos first, BlockPos second) {
        this.min = new BlockPos(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ())
        );

        this.max = new BlockPos(
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ())
        );

        this.xSize = this.max.getX() - this.min.getX() + 1;
        this.ySize = this.max.getY() - this.min.getY() + 1;
        this.zSize = this.max.getZ() - this.min.getZ() + 1;
    }

    // Creates an area renderable from two selected corners.
    public static AreaRenderable of(BlockPos first, BlockPos second) {
        return new AreaRenderable(first, second);
    }

    // Emits all non-air blocks and block entities inside the selected area.
    @Override
    public void emitVertices(PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta) {
        var minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        poseStack.pushPose();

        // Center the selected area around the render origin.
        poseStack.translate(-this.xSize / 2.0F, -this.ySize / 2.0F, -this.zSize / 2.0F);

        for (BlockPos worldPos : BlockPos.betweenClosed(this.min, this.max)) {
            BlockState state = minecraft.level.getBlockState(worldPos);

            if (state.isAir()) {
                continue;
            }

            int localX = worldPos.getX() - this.min.getX();
            int localY = worldPos.getY() - this.min.getY();
            int localZ = worldPos.getZ() - this.min.getZ();

            poseStack.pushPose();
            poseStack.translate(localX, localY, localZ);

            minecraft.getBlockRenderer().renderSingleBlock(
                    state,
                    poseStack,
                    bufferSource,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY
            );

            BlockEntity blockEntity = minecraft.level.getBlockEntity(worldPos);
            if (blockEntity != null) {
                minecraft.getBlockEntityRenderDispatcher().renderItem(
                        blockEntity,
                        poseStack,
                        bufferSource,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY
                );
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    // Returns area-specific transform properties.
    @Override
    public AreaPropertyBundle properties() {
        return AreaPropertyBundle.INSTANCE;
    }

    // Area renders use a shared output path.
    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.of("area_renders", "area_render");
    }

    // Returns the selected block count.
    public int blockCount() {
        return this.xSize * this.ySize * this.zSize;
    }

    /**
     * Area-specific transform properties.
     */
    public static class AreaPropertyBundle extends DefaultPropertyBundle {
        public static final AreaPropertyBundle INSTANCE = new AreaPropertyBundle();

        private AreaPropertyBundle() {
            this.scale.setDefaultValue(50).setToDefault();
            this.slant.setDefaultValue(30).setToDefault();
            this.rotation.setDefaultValue(135).setToDefault();
        }

        // Applies structure-friendly scale and offsets.
        @Override
        public void applyToViewMatrix(org.joml.Matrix4fStack modelViewStack) {
            float scale = this.scale.get() / 1000.0F;
            modelViewStack.scale(scale, scale, scale);

            modelViewStack.translate(this.xOffset.get() / 2600.0F, this.yOffset.get() / -2600.0F, 0.0F);

            modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get()));
            modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get()));

            this.updateAndApplyRotationOffset(modelViewStack);
        }
    }
}
