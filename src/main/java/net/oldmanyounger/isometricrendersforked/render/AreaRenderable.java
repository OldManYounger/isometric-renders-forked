package net.oldmanyounger.isometricrendersforked.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.oldmanyounger.isometricrendersforked.IsometricRendersForked;
import net.oldmanyounger.isometricrendersforked.property.DefaultPropertyBundle;
import net.oldmanyounger.isometricrendersforked.util.ExportPathSpec;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;

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

    // Copies a real world block entity into local area-render coordinates.
    private static @Nullable BlockEntity copyBlockEntityForAreaRender(Level level, BlockPos worldPos, BlockPos localPos, BlockState state) {
        BlockEntity originalBlockEntity = level.getBlockEntity(worldPos);

        if (originalBlockEntity == null) {
            return null;
        }

        CompoundTag nbt = originalBlockEntity.saveWithFullMetadata(level.registryAccess());

        // Force local coordinates so block entity renderers evaluate the render snapshot position.
        nbt.putInt("x", localPos.getX());
        nbt.putInt("y", localPos.getY());
        nbt.putInt("z", localPos.getZ());

        BlockEntity copiedBlockEntity = BlockEntity.loadStatic(localPos, state, nbt, level.registryAccess());

        if (copiedBlockEntity == null) {
            IsometricRendersForked.LOGGER.warn("Failed to copy block entity for area render at {}", worldPos);
            return null;
        }

        copiedBlockEntity.setLevel(level);
        copiedBlockEntity.setBlockState(state);

        return copiedBlockEntity;
    }

    // Applies balanced block lighting so rotated block faces do not fall into one-sided shadow.
    @Override
    public void setupLighting(Matrix4f modelViewMatrix) {
        Matrix4f lightTransform = new Matrix4f(modelViewMatrix);
        lightTransform.invert();

        Vector4f keyLight = new Vector4f(0.35F, 0.75F, 1.0F, 0.0F);
        Vector4f fillLight = new Vector4f(-0.65F, -0.35F, -0.55F, 0.0F);

        keyLight.mul(lightTransform);
        fillLight.mul(lightTransform);

        RenderSystem.setShaderLights(
                new Vector3f(keyLight.x, keyLight.y, keyLight.z).normalize(),
                new Vector3f(fillLight.x, fillLight.y, fillLight.z).normalize()
        );
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

            BlockPos localPos = new BlockPos(localX, localY, localZ);
            BlockEntity blockEntity = copyBlockEntityForAreaRender(minecraft.level, worldPos, localPos, state);

            // Pass block entity model data into NeoForge's block model path.
            ModelData modelData = blockEntity == null ? ModelData.EMPTY : blockEntity.getModelData();

            minecraft.getBlockRenderer().renderSingleBlock(
                    state,
                    poseStack,
                    bufferSource,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    modelData,
                    null
            );

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
