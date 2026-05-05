package net.oldmanyounger.isometricrendersforked.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.oldmanyounger.isometricrendersforked.property.PropertyBundle;
import net.oldmanyounger.isometricrendersforked.util.ExportPathSpec;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * No-op renderable used when no real render target is selected.
 */
public class EmptyRenderable implements Renderable<PropertyBundle> {
    private static final PropertyBundle EMPTY_BUNDLE = new PropertyBundle() {
        // The empty bundle does not modify the view matrix.
        @Override
        public void applyToViewMatrix(Matrix4fStack modelViewStack) {}
    };

    // Emits no vertices.
    @Override
    public void emitVertices(PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta) {}

    // Performs no draw work.
    @Override
    public void draw(Matrix4f modelViewMatrix) {}

    // Returns the no-op property bundle.
    @Override
    public PropertyBundle properties() {
        return EMPTY_BUNDLE;
    }

    // Returns a placeholder export path.
    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.of("", "empty");
    }
}
