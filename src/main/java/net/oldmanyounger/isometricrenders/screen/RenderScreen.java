package net.oldmanyounger.isometricrenders.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.oldmanyounger.isometricrenders.property.DefaultPropertyBundle;
import net.oldmanyounger.isometricrenders.property.GlobalProperties;
import net.oldmanyounger.isometricrenders.property.IntProperty;
import net.oldmanyounger.isometricrenders.render.EntityRenderable;
import net.oldmanyounger.isometricrenders.render.Renderable;
import net.oldmanyounger.isometricrenders.render.RenderableDispatcher;
import net.oldmanyounger.isometricrenders.render.TooltipRenderable;
import net.oldmanyounger.isometricrenders.util.ImageIO;
import net.oldmanyounger.isometricrenders.util.Translate;

/**
 * Vanilla preview and control screen for renderables.
 *
 * <p>This screen is the first no-owo GUI pass for the NeoForge port. It keeps
 * the existing preview and export behavior while adding clickable controls for
 * the most important transform properties.</p>
 */
public class RenderScreen extends Screen {
    private static final int PANEL_WIDTH = 176;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;

    private final Renderable<?> renderable;
    private Button adjustmentModeButton;
    private boolean fineAdjustments = false;
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

    // ==================================
    //  WIDGET SETUP
    // ==================================

    // Creates vanilla controls for the current render target.
    @Override
    protected void init() {
        int panelX = this.panelX();
        int y = 32;

        this.addRenderableWidget(this.button(panelX, y, 84, BUTTON_HEIGHT, Translate.gui("control.export"), button -> this.exportPng()));
        this.addRenderableWidget(this.button(panelX + 88, y, 84, BUTTON_HEIGHT, Translate.gui("control.done"), button -> this.onClose()));
        y += ROW_HEIGHT;

        if (this.renderable instanceof TooltipRenderable) {
            return;
        }

        if (this.renderable.properties() instanceof DefaultPropertyBundle properties) {
            this.addRenderableWidget(this.button(panelX, y, 84, BUTTON_HEIGHT, Translate.gui("control.reset"), button -> this.resetDefaultProperties(properties)));
            this.adjustmentModeButton = this.addRenderableWidget(this.button(panelX + 88, y, 84, BUTTON_HEIGHT, this.adjustmentModeLabel(), button -> this.toggleAdjustmentMode()));
            y += ROW_HEIGHT + 4;

            y = this.addStepper(panelX, y, properties.scale, -10, 10, -1, 1);
            y = this.addStepper(panelX, y, properties.rotation, -15, 15, -1, 1);
            y = this.addStepper(panelX, y, properties.slant, -5, 5, -1, 1);
            y = this.addStepper(panelX, y, properties.xOffset, -100, 100, -10, 10);
            y = this.addStepper(panelX, y, properties.yOffset, -100, 100, -10, 10);

            if (properties instanceof EntityRenderable.EntityPropertyBundle entityProperties) {
                y += 4;
                y = this.addStepper(panelX, y, entityProperties.yaw, -15, 15, -1, 1);
                this.addStepper(panelX, y, entityProperties.pitch, -5, 5, -1, 1);
            }
        }
    }

    // Adds a two-button stepper row for a property.
    private int addStepper(int x, int y, IntProperty property, int coarseDecrease, int coarseIncrease, int fineDecrease, int fineIncrease) {
        this.addRenderableWidget(this.button(x, y, 38, BUTTON_HEIGHT, Component.literal("-"), button ->
                property.modify(this.adjustmentDelta(coarseDecrease, fineDecrease))
        ));

        this.addRenderableWidget(this.button(x + 134, y, 38, BUTTON_HEIGHT, Component.literal("+"), button ->
                property.modify(this.adjustmentDelta(coarseIncrease, fineIncrease))
        ));

        return y + ROW_HEIGHT;
    }

    // Creates a vanilla button with consistent sizing.
    private Button button(int x, int y, int width, int height, Component label, Button.OnPress onPress) {
        return Button.builder(label, onPress)
                .bounds(x, y, width, height)
                .build();
    }

    // ==================================
    //  RENDERING
    // ==================================

    // Draws the screen background, renderable preview, controls, and status text.
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.lastPartialTick = partialTick;

        // Use the transparent background path because Screen.renderBackground applies
        // Minecraft's menu blur shader, which blurs the preview and control panel.
        this.renderTransparentBackground(guiGraphics);

        if (this.renderable instanceof TooltipRenderable tooltipRenderable) {
            tooltipRenderable.renderTooltip(guiGraphics, this.width, this.height);
        } else {
            // Flush GUI batches before temporarily replacing projection/model-view state.
            guiGraphics.flush();

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            float aspectRatio = this.height == 0 ? 1.0F : this.width / (float) this.height;
            RenderableDispatcher.drawIntoActiveFramebuffer(this.renderable, aspectRatio, partialTick, modelViewStack ->
                    modelViewStack.translate(this.previewXOffset(), 0.0F, 0.0F)
            );
        }

        this.renderPreviewFrame(guiGraphics);
        this.renderControlPanel(guiGraphics);

        // Render vanilla widgets manually. Calling super.render would draw the blurred
        // menu background a second time over the preview.
        for (net.minecraft.client.gui.components.Renderable widget : this.renderables) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        guiGraphics.drawCenteredString(this.font, this.title, this.previewCenterX(), 12, 0xFFFFFFFF);
    }

    // Draws control labels and current values beside the vanilla buttons.
    private void renderControlPanel(GuiGraphics guiGraphics) {
        int panelX = this.panelX();
        int panelRight = panelX + PANEL_WIDTH;
        int panelBottom = this.height - 44;

        guiGraphics.fill(panelX - 6, 26, panelRight + 6, panelBottom, 0xAA101010);
        guiGraphics.drawString(this.font, Translate.gui("control.panel"), panelX, 14, 0xFFFFFFFF, false);

        if (this.renderable instanceof TooltipRenderable) {
            guiGraphics.drawCenteredString(this.font, Translate.gui("tooltip_status"), panelX + PANEL_WIDTH / 2, 62, 0xFFAAAAAA);
            return;
        }

        if (this.renderable.properties() instanceof DefaultPropertyBundle properties) {
            int y = 84;

            y = this.drawStepperLabel(guiGraphics, y, Translate.gui("control.scale"), properties.scale.get());
            y = this.drawStepperLabel(guiGraphics, y, Translate.gui("control.rotation"), properties.rotation.get());
            y = this.drawStepperLabel(guiGraphics, y, Translate.gui("control.slant"), properties.slant.get());
            y = this.drawStepperLabel(guiGraphics, y, Translate.gui("control.x_offset"), properties.xOffset.get());
            y = this.drawStepperLabel(guiGraphics, y, Translate.gui("control.y_offset"), properties.yOffset.get());

            if (properties instanceof EntityRenderable.EntityPropertyBundle entityProperties) {
                y += 4;
                y = this.drawStepperLabel(guiGraphics, y, Translate.gui("control.entity_yaw"), entityProperties.yaw.get());
                this.drawStepperLabel(guiGraphics, y, Translate.gui("control.entity_pitch"), entityProperties.pitch.get());
            }
        }
    }

    // Draws a subtle frame showing the square projection used by PNG exports.
    private void renderPreviewFrame(GuiGraphics guiGraphics) {
        if (this.renderable instanceof TooltipRenderable) {
            return;
        }

        int size = this.previewFrameSize();
        int centerX = this.previewCenterX();
        int centerY = this.height / 2;
        int left = centerX - size / 2;
        int top = centerY - size / 2;
        int right = left + size;
        int bottom = top + size;
        int borderColor = 0x55FFFFFF;
        int centerColor = 0x66FFFFFF;

        // This frame represents the square export projection, not a clipping mask.
        guiGraphics.fill(left, top, right, top + 1, borderColor);
        guiGraphics.fill(left, bottom - 1, right, bottom, borderColor);
        guiGraphics.fill(left, top, left + 1, bottom, borderColor);
        guiGraphics.fill(right - 1, top, right, bottom, borderColor);

        // Draw a small center mark so offsets have an obvious reference point.
        guiGraphics.fill(centerX - 5, centerY, centerX + 6, centerY + 1, centerColor);
        guiGraphics.fill(centerX, centerY - 5, centerX + 1, centerY + 6, centerColor);
    }

    // Draws one centered property label between a stepper's minus and plus buttons.
    private int drawStepperLabel(GuiGraphics guiGraphics, int y, Component label, int value) {
        int centerX = this.panelX() + PANEL_WIDTH / 2;
        Component text = Component.literal("")
                .append(label)
                .append(Component.literal(": " + value));

        guiGraphics.drawCenteredString(this.font, text, centerX, y + 6, 0xFFE0E0E0);
        return y + ROW_HEIGHT;
    }

    // Computes the left edge of the right-side control panel.
    private int panelX() {
        return Math.max(8, this.width - PANEL_WIDTH - 14);
    }

    // Computes the horizontal offset needed to center previews left of the controls.
    private float previewXOffset() {
        return (this.previewCenterX() - this.width / 2.0F) * 2.0F / Math.max(1.0F, this.height);
    }

    // Returns the x-coordinate where renderables should visually center.
    private int previewCenterX() {
        int availableRight = Math.max(120, this.panelX() - 18);
        return availableRight / 2;
    }

    // Returns the on-screen size of the square export projection.
    private int previewFrameSize() {
        return Math.max(1, this.height - 2);
    }

    // ==================================
    //  STATE
    // ==================================

    // Toggles between coarse and fine GUI adjustment steps.
    private void toggleAdjustmentMode() {
        this.fineAdjustments = !this.fineAdjustments;

        if (this.adjustmentModeButton != null) {
            this.adjustmentModeButton.setMessage(this.adjustmentModeLabel());
        }
    }

    // Returns the label for the current adjustment mode.
    private Component adjustmentModeLabel() {
        return this.fineAdjustments
                ? Translate.gui("control.fine")
                : Translate.gui("control.coarse");
    }

    // Chooses the current step amount for a stepper click.
    private int adjustmentDelta(int coarse, int fine) {
        return this.fineAdjustments ? fine : coarse;
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
        if (this.renderable instanceof TooltipRenderable) {
            return Translate.gui("tooltip_status");
        }

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
        var image = this.renderable instanceof TooltipRenderable tooltipRenderable
                ? tooltipRenderable.drawIntoImage(GlobalProperties.exportResolution)
                : RenderableDispatcher.drawIntoImage(this.renderable, this.lastPartialTick, GlobalProperties.exportResolution);

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
