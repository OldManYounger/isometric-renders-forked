package net.oldmanyounger.isometricrenders.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.oldmanyounger.isometricrenders.property.PropertyBundle;
import net.oldmanyounger.isometricrenders.util.ExportPathSpec;
import net.oldmanyounger.isometricrenders.util.ParticleRestriction;
import org.joml.Matrix4f;

/**
 * Base abstraction for anything Isometric Renders can preview or export.
 *
 * <p>Individual implementations provide vertices, draw behavior, render
 * properties, particle restrictions, and their default export path.</p>
 *
 * @param <P> property bundle type used by this renderable
 */
public interface Renderable<P extends PropertyBundle> {
    Renderable<PropertyBundle> EMPTY = new EmptyRenderable();

    // Performs setup before rendering begins.
    default void prepare() {}

    // Applies custom lighting before this renderable draws.
    default void setupLighting(Matrix4f modelViewMatrix) {}

    // Emits vertices into the active buffer source.
    void emitVertices(PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta);

    // Flushes or performs draw work after vertices have been emitted.
    void draw(Matrix4f modelViewMatrix);

    // Cleans temporary state after a render pass.
    default void cleanUp() {}

    // Releases long-lived resources when the renderable is no longer needed.
    default void dispose() {}

    // Returns the particle restriction used while this renderable is active.
    default ParticleRestriction<?> particleRestriction() {
        return ParticleRestriction.never();
    }

    // Returns this renderable's editable properties.
    P properties();

    // Returns this renderable's default export path.
    ExportPathSpec exportPath();
}
