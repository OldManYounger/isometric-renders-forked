package net.oldmanyounger.isometricrenders.property;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4fStack;

/**
 * Standard transform controls used by most renderables.
 *
 * <p>This bundle owns scale, rotation, slant, lighting angle, offsets, and
 * optional automatic rotation. GUI control creation is deferred until the
 * NeoForge UI path is ported.</p>
 */
public class DefaultPropertyBundle implements PropertyBundle {
    private static final DefaultPropertyBundle INSTANCE = new DefaultPropertyBundle();

    public final IntProperty scale = IntProperty.of(100, 0, 500);
    public final IntProperty rotation = IntProperty.of(135, 0, 360).withRollover();
    public final IntProperty slant = IntProperty.of(30, -90, 90);
    public final IntProperty lightAngle = IntProperty.of(-45, -45, 45);

    public final IntProperty xOffset = IntProperty.of(0, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
    public final IntProperty yOffset = IntProperty.of(0, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);

    public final IntProperty rotationSpeed = IntProperty.of(0, 0, 100);

    protected float rotationOffset = 0.0F;
    protected boolean rotationOffsetUpdated = false;

    // Applies scale, offset, slant, rotation, and animated rotation to the view matrix.
    @Override
    public void applyToViewMatrix(Matrix4fStack modelViewStack) {
        float scale = this.scale.get() / 100.0F;
        modelViewStack.scale(scale, scale, scale);

        modelViewStack.translate(this.xOffset.get() / 26000.0F, this.yOffset.get() / -26000.0F, 0.0F);

        modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get()));
        modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get()));

        this.updateAndApplyRotationOffset(modelViewStack);
    }

    // Returns the current animated rotation offset.
    public float rotationOffset() {
        return this.rotationOffset;
    }

    // Lets the future render dispatcher reset per-frame animation bookkeeping.
    public void resetFrameState() {
        this.rotationOffsetUpdated = false;
    }

    // Updates animated rotation once per frame and applies it to the view matrix.
    protected void updateAndApplyRotationOffset(Matrix4fStack modelViewStack) {
        if (this.rotationSpeed.get() != 0) {
            if (!this.rotationOffsetUpdated) {
                this.rotationOffset += Minecraft.getInstance().getTimer().getRealtimeDeltaTicks() * this.rotationSpeed.get() * 0.1F;
                this.rotationOffsetUpdated = true;
            }

            modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotationOffset));
        } else {
            this.rotationOffset = 0.0F;
        }
    }

    // Returns the shared default property bundle.
    public static DefaultPropertyBundle get() {
        return INSTANCE;
    }
}
