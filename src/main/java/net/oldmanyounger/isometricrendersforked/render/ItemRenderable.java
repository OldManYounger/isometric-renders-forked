package net.oldmanyounger.isometricrendersforked.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.oldmanyounger.isometricrendersforked.property.DefaultPropertyBundle;
import net.oldmanyounger.isometricrendersforked.util.ExportPathSpec;
import org.joml.Matrix4fStack;

/**
 * Renderable wrapper for a single item stack.
 *
 * <p>This is the first concrete render target in the NeoForge port. It relies
 * on Minecraft's normal item renderer so vanilla and modded item models follow
 * the same rendering path they use in-game.</p>
 */
public class ItemRenderable extends DefaultRenderable<DefaultPropertyBundle> {
    private static final DefaultPropertyBundle PROPERTIES = new DefaultPropertyBundle() {
        // Applies item-specific defaults before normal animated rotation.
        @Override
        public void applyToViewMatrix(Matrix4fStack modelViewStack) {
            float scale = this.scale.get() / 100.0F * 1.75F;
            modelViewStack.scale(scale, scale, scale);

            modelViewStack.translate(this.xOffset.get() / 26000.0F, this.yOffset.get() / -26000.0F, 0.0F);

            modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get()));
            modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get()));

            this.updateAndApplyRotationOffset(modelViewStack);
        }
    };

    static {
        PROPERTIES.slant.setDefaultValue(0).setToDefault();
        PROPERTIES.rotation.setDefaultValue(0).setToDefault();
    }

    private final ItemStack stack;

    /**
     * Creates an item renderable.
     *
     * @param stack the stack to render
     */
    public ItemRenderable(ItemStack stack) {
        this.stack = stack.copy();
    }

    // Emits this item's vertices using Minecraft's item renderer.
    @Override
    public void emitVertices(PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta) {
        var minecraft = Minecraft.getInstance();

        minecraft.getItemRenderer().renderStatic(
                this.stack,
                ItemDisplayContext.NONE,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                minecraft.level,
                0
        );
    }

    // Returns the shared item transform properties.
    @Override
    public DefaultPropertyBundle properties() {
        return PROPERTIES;
    }

    // Builds the default export path for this item.
    @Override
    public ExportPathSpec exportPath() {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(this.stack.getItem());

        if (itemId == null) {
            itemId = ResourceLocation.fromNamespaceAndPath("unknown", "item");
        }

        return ExportPathSpec.ofIdentified(itemId, "item");
    }

    // Returns a defensive copy of the rendered stack.
    public ItemStack stack() {
        return this.stack.copy();
    }
}
