package net.oldmanyounger.isometricrendersforked.property;

import org.joml.Matrix4fStack;

/**
 * A group of render properties that can be applied to a render view matrix.
 *
 * <p>The original Fabric mod also lets property bundles build owo-ui controls.
 * That UI hook is intentionally deferred until the NeoForge owo-lib dependency
 * decision is made.</p>
 */
public interface PropertyBundle {
    // Applies this bundle's transform settings to the model-view stack.
    void applyToViewMatrix(Matrix4fStack modelViewStack);
}
