package net.oldmanyounger.isometricrenders.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.oldmanyounger.isometricrenders.IsometricRendersForked;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.function.Consumer;

/**
 * Draws renderables through Minecraft's active render pipeline.
 *
 * <p>The dispatcher can draw into the current framebuffer for preview rendering
 * or into a temporary off-screen framebuffer for PNG export.</p>
 */
public final class RenderableDispatcher {
    private RenderableDispatcher() {}

    // Draws a renderable into the active framebuffer with no extra transform.
    public static void drawIntoActiveFramebuffer(Renderable<?> renderable, float aspectRatio, float tickDelta) {
        drawIntoActiveFramebuffer(renderable, aspectRatio, tickDelta, modelViewStack -> {});
    }

    // Draws a renderable into the active framebuffer with an extra caller-supplied transform.
    public static void drawIntoActiveFramebuffer(
            Renderable<?> renderable,
            float aspectRatio,
            float tickDelta,
            Consumer<Matrix4fStack> transformer
    ) {
        renderable.prepare();

        var modelViewStack = RenderSystem.getModelViewStack();

        RenderSystem.backupProjectionMatrix();
        modelViewStack.pushMatrix();

        try {
            modelViewStack.identity();

            transformer.accept(modelViewStack);
            renderable.properties().applyToViewMatrix(modelViewStack);

            RenderSystem.applyModelViewMatrix();

            var projectionMatrix = new Matrix4f().setOrtho(
                    -aspectRatio,
                    aspectRatio,
                    -1.0F,
                    1.0F,
                    -1000.0F,
                    3000.0F
            );

            RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);

            var activeModelView = new Matrix4f(modelViewStack);

            renderable.setupLighting(activeModelView);

            IsometricRendersForked.beginRenderableDraw();

            try {
                renderable.emitVertices(
                        new PoseStack(),
                        Minecraft.getInstance().renderBuffers().bufferSource(),
                        tickDelta
                );

                renderable.draw(activeModelView);
            } finally {
                IsometricRendersForked.endRenderableDraw();
            }
        } finally {
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();

            renderable.cleanUp();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    // Draws the renderable into a new native image at the requested square resolution.
    public static NativeImage drawIntoImage(Renderable<?> renderable, float tickDelta, int size) {
        return copyFramebufferIntoImage(drawIntoTexture(renderable, tickDelta, size));
    }

    // Draws the renderable into a temporary off-screen framebuffer.
    public static RenderTarget drawIntoTexture(Renderable<?> renderable, float tickDelta, int size) {
        var minecraft = Minecraft.getInstance();
        var previousTarget = minecraft.getMainRenderTarget();

        var target = new TextureTarget(size, size, true, Minecraft.ON_OSX);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);

        try {
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            drawIntoActiveFramebuffer(renderable, 1.0F, tickDelta);
        } finally {
            target.unbindWrite();
            previousTarget.bindWrite(true);
        }

        return target;
    }

    // Copies a framebuffer's color texture into system memory.
    public static NativeImage copyFramebufferIntoImage(RenderTarget target) {
        var image = new NativeImage(target.width, target.height, false);

        target.bindRead();
        image.downloadTexture(0, false);
        image.flipY();
        target.unbindRead();

        target.destroyBuffers();

        return image;
    }
}
