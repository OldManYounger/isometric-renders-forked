package net.oldmanyounger.isometricrenders.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.oldmanyounger.isometricrenders.property.DefaultPropertyBundle;
import net.oldmanyounger.isometricrenders.property.GlobalProperties;
import net.oldmanyounger.isometricrenders.render.EntityRenderable;
import net.oldmanyounger.isometricrenders.render.Renderable;
import net.oldmanyounger.isometricrenders.render.RenderableDispatcher;
import net.oldmanyounger.isometricrenders.util.ImageIO;
import net.oldmanyounger.isometricrenders.util.Translate;
import org.lwjgl.glfw.GLFW;

/**
 * Temporary vanilla preview screen for renderables.
 *
 * <p>The original mod uses owo-ui for the final control-heavy render screen.
 * This screen exists to validate the NeoForge render and export paths before
 * the final UI dependency is introduced.</p>
 */
public class RenderScreen extends Screen {
    private final Renderable<?> renderable;
    private float lastPartialTick = 0.0F;

    /**
     * Creates a render preview screen.
     *
     * @param renderable the renderable to preview
     */
    public RenderScreen(Renderable<?> renderable) {
        super(Translate.gui("render_screen"));
        this.renderable = renderable;
    }

    // Draws the screen background, renderable preview, and temporary status text.
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.lastPartialTick = partialTick;
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Flush GUI batches before temporarily replacing projection/model-view state.
        guiGraphics.flush();

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float aspectRatio = this.height == 0 ? 1.0F : this.width / (float) this.height;
        RenderableDispatcher.drawIntoActiveFramebuffer(this.renderable, aspectRatio, partialTick);

        // Continue normal GUI drawing after the custom render pass.
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, this.transformStatus(), this.width / 2, this.height - 34, 0xFFAAAAAA);
        guiGraphics.drawCenteredString(this.font, ImageIO.progressText(), this.width / 2, this.height - 22, 0xFFAAAAAA);
    }

    // Handles temporary preview-screen hotkeys.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F12) {
            this.exportPng();
            return true;
        }

        if (this.handleTransformKey(keyCode, modifiers)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Applies temporary keyboard controls to the active render properties.
    private boolean handleTransformKey(int keyCode, int modifiers) {
        if (!(this.renderable.properties() instanceof DefaultPropertyBundle properties)) {
            return false;
        }

        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        int rotationStep = shift ? 45 : 15;
        int slantStep = shift ? 15 : 5;
        int scaleStep = shift ? 50 : 10;
        int offsetStep = shift ? 500 : 100;

        switch (keyCode) {
            case GLFW.GLFW_KEY_A -> properties.rotation.modify(-rotationStep);
            case GLFW.GLFW_KEY_D -> properties.rotation.modify(rotationStep);
            case GLFW.GLFW_KEY_W -> properties.slant.modify(slantStep);
            case GLFW.GLFW_KEY_S -> properties.slant.modify(-slantStep);
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> properties.scale.modify(scaleStep);
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> properties.scale.modify(-scaleStep);
            case GLFW.GLFW_KEY_LEFT -> properties.xOffset.modify(-offsetStep);
            case GLFW.GLFW_KEY_RIGHT -> properties.xOffset.modify(offsetStep);
            case GLFW.GLFW_KEY_UP -> properties.yOffset.modify(offsetStep);
            case GLFW.GLFW_KEY_DOWN -> properties.yOffset.modify(-offsetStep);
            case GLFW.GLFW_KEY_R -> this.resetDefaultProperties(properties);
            case GLFW.GLFW_KEY_J -> {
                if (properties instanceof EntityRenderable.EntityPropertyBundle entityProperties) {
                    entityProperties.yaw.modify(-rotationStep);
                } else {
                    return false;
                }
            }
            case GLFW.GLFW_KEY_L -> {
                if (properties instanceof EntityRenderable.EntityPropertyBundle entityProperties) {
                    entityProperties.yaw.modify(rotationStep);
                } else {
                    return false;
                }
            }
            case GLFW.GLFW_KEY_I -> {
                if (properties instanceof EntityRenderable.EntityPropertyBundle entityProperties) {
                    entityProperties.pitch.modify(slantStep);
                } else {
                    return false;
                }
            }
            case GLFW.GLFW_KEY_K -> {
                if (properties instanceof EntityRenderable.EntityPropertyBundle entityProperties) {
                    entityProperties.pitch.modify(-slantStep);
                } else {
                    return false;
                }
            }
            default -> {
                return false;
            }
        }

        return true;
    }

    // Resets the common transform properties to their defaults.
    private void resetDefaultProperties(DefaultPropertyBundle properties) {
        properties.scale.setToDefault();
        properties.rotation.setToDefault();
        properties.slant.setToDefault();
        properties.lightAngle.setToDefault();
        properties.xOffset.setToDefault();
        properties.yOffset.setToDefault();
        properties.rotationSpeed.setToDefault();

        if (properties instanceof EntityRenderable.EntityPropertyBundle entityProperties) {
            entityProperties.yaw.setToDefault();
            entityProperties.pitch.setToDefault();
        }
    }

    // Builds the temporary transform status line.
    private Component transformStatus() {
        if (!(this.renderable.properties() instanceof DefaultPropertyBundle properties)) {
            return Translate.gui("preview_only");
        }

        if (properties instanceof EntityRenderable.EntityPropertyBundle entityProperties) {
            return Translate.gui(
                    "transform_status_entity",
                    properties.scale.get(),
                    properties.rotation.get(),
                    properties.slant.get(),
                    entityProperties.yaw.get(),
                    entityProperties.pitch.get()
            );
        }

        return Translate.gui(
                "transform_status",
                properties.scale.get(),
                properties.rotation.get(),
                properties.slant.get()
        );
    }

    // Exports the current renderable to a PNG.
    private void exportPng() {
        var image = RenderableDispatcher.drawIntoImage(this.renderable, this.lastPartialTick, GlobalProperties.exportResolution);

        ImageIO.save(image, this.renderable.exportPath()).whenComplete((file, throwable) -> {
            Minecraft.getInstance().execute(() -> {
                var player = Minecraft.getInstance().player;

                if (player == null) {
                    return;
                }

                if (throwable == null) {
                    player.displayClientMessage(Translate.msg("exported_as", file.getPath()), false);
                } else {
                    player.displayClientMessage(Translate.msg("export_failed", throwable.getMessage()), false);
                }
            });
        });
    }

    // Releases renderable resources when the preview closes.
    @Override
    public void onClose() {
        this.renderable.dispose();
        super.onClose();
    }

    // The preview should not pause an active world.
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
