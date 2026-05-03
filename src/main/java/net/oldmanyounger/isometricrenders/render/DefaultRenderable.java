package net.oldmanyounger.isometricrenders.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.oldmanyounger.isometricrenders.property.DefaultPropertyBundle;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Base implementation for renderables using the standard property bundle.
 *
 * <p>The lighting behavior mirrors the original mod: the light vector is
 * transformed by the inverse model-view matrix so lighting stays visually
 * stable as the render target rotates.</p>
 *
 * @param <P> default property bundle subtype used by this renderable
 */
public abstract class DefaultRenderable<P extends DefaultPropertyBundle> implements Renderable<P> {
    // Applies transformed shader lighting for this renderable.
    @Override
    public void setupLighting(Matrix4f modelViewMatrix) {
        var lightDirection = this.getLightDirection();
        var lightTransform = new Matrix4f(modelViewMatrix);

        lightTransform.invert();
        lightDirection.mul(lightTransform);

        var transformedLightDirection = new Vector3f(lightDirection.x, lightDirection.y, lightDirection.z);
        RenderSystem.setShaderLights(transformedLightDirection, transformedLightDirection);
    }

    // Flushes the main entity buffer source after vertices have been emitted.
    @Override
    public void draw(Matrix4f modelViewMatrix) {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }

    // Computes the untransformed light direction from the active properties.
    protected Vector4f getLightDirection() {
        return new Vector4f(this.properties().lightAngle.get() / 90.0F, 0.35F, 1.0F, 0.0F);
    }
}
