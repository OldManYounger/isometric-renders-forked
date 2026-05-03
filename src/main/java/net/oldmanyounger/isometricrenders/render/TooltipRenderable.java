package net.oldmanyounger.isometricrenders.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.oldmanyounger.isometricrenders.property.DefaultPropertyBundle;
import net.oldmanyounger.isometricrenders.util.ExportPathSpec;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renderable wrapper for an item tooltip.
 *
 * <p>Tooltips are rendered through Minecraft's GUI tooltip components instead
 * of the 3D renderable dispatcher. This keeps text, item components, and
 * NeoForge tooltip extensions on the same path used by normal item hovers.</p>
 */
public class TooltipRenderable extends DefaultRenderable<DefaultPropertyBundle> {
    private static final DefaultPropertyBundle PROPERTIES = new DefaultPropertyBundle();

    private final ItemStack stack;

    /**
     * Creates a tooltip renderable.
     *
     * @param stack the item stack whose tooltip should be rendered
     */
    public TooltipRenderable(ItemStack stack) {
        this.stack = stack.copy();
    }

    // Tooltip renderables do not emit 3D vertices.
    @Override
    public void emitVertices(PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta) {}

    // Returns the shared placeholder properties.
    @Override
    public DefaultPropertyBundle properties() {
        return PROPERTIES;
    }

    // Builds the default export path for this tooltip.
    @Override
    public ExportPathSpec exportPath() {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(this.stack.getItem());

        if (itemId == null) {
            itemId = ResourceLocation.fromNamespaceAndPath("unknown", "item");
        }

        return ExportPathSpec.ofIdentified(itemId, "tooltip");
    }

    // Draws the tooltip centered in the current GUI.
    public void renderTooltip(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        var minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        List<ClientTooltipComponent> components = this.tooltipComponents(screenWidth, screenHeight, font);

        if (components.isEmpty()) {
            return;
        }

        TooltipSize size = measure(components, font);
        int x = Math.max(4, (screenWidth - size.width()) / 2);
        int y = Math.max(4, (screenHeight - size.height()) / 2);

        this.renderComponents(guiGraphics, font, components, x, y, size.width(), size.height());
    }

    // Draws this tooltip into a transparent square image for PNG export.
    public NativeImage drawIntoImage(int size) {
        var minecraft = Minecraft.getInstance();
        RenderTarget previousTarget = minecraft.getMainRenderTarget();

        var target = new TextureTarget(size, size, true, Minecraft.ON_OSX);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);

        var modelViewStack = RenderSystem.getModelViewStack();

        RenderSystem.backupProjectionMatrix();
        modelViewStack.pushMatrix();

        try {
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            modelViewStack.identity();
            RenderSystem.applyModelViewMatrix();

            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0.0F, (float) size, (float) size, 0.0F, -1000.0F, 3000.0F),
                    VertexSorting.ORTHOGRAPHIC_Z
            );

            GuiGraphics guiGraphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            this.renderTooltip(guiGraphics, size, size);
            guiGraphics.flush();
        } finally {
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();

            target.unbindWrite();
            previousTarget.bindWrite(true);
        }

        return RenderableDispatcher.copyFramebufferIntoImage(target);
    }

    // Builds NeoForge-aware tooltip components for this stack.
    private List<ClientTooltipComponent> tooltipComponents(int screenWidth, int screenHeight, Font font) {
        var minecraft = Minecraft.getInstance();

        return ClientHooks.gatherTooltipComponents(
                this.stack,
                Screen.getTooltipFromItem(minecraft, this.stack),
                this.stack.getTooltipImage(),
                screenWidth / 2,
                screenWidth,
                screenHeight,
                font
        );
    }

    // Measures tooltip dimensions using vanilla's height rules.
    private static TooltipSize measure(List<ClientTooltipComponent> components, Font font) {
        int width = 0;
        int height = components.size() == 1 ? -2 : 0;

        for (ClientTooltipComponent component : components) {
            width = Math.max(width, component.getWidth(font));
            height += component.getHeight();
        }

        return new TooltipSize(width, height);
    }

    // Renders tooltip background, text, and image components.
    private void renderComponents(GuiGraphics guiGraphics, Font font, List<ClientTooltipComponent> components, int x, int y, int width, int height) {
        TooltipRenderUtil.renderTooltipBackground(guiGraphics, x, y, width, height, 400);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);

        int textY = y;
        for (int index = 0; index < components.size(); index++) {
            ClientTooltipComponent component = components.get(index);
            component.renderText(font, x, textY, guiGraphics.pose().last().pose(), guiGraphics.bufferSource());
            textY += component.getHeight() + (index == 0 ? 2 : 0);
        }

        guiGraphics.flush();

        int imageY = y;
        for (int index = 0; index < components.size(); index++) {
            ClientTooltipComponent component = components.get(index);
            component.renderImage(font, x, imageY, guiGraphics);
            imageY += component.getHeight() + (index == 0 ? 2 : 0);
        }

        guiGraphics.pose().popPose();
        guiGraphics.flush();
    }

    // Returns a defensive copy of the tooltip stack.
    public ItemStack stack() {
        return this.stack.copy();
    }

    /**
     * Pixel dimensions for a rendered tooltip.
     *
     * @param width measured tooltip width
     * @param height measured tooltip height
     */
    private record TooltipSize(int width, int height) {}
}
