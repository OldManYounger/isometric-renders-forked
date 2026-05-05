package net.oldmanyounger.isometricrendersforked.render;

import net.oldmanyounger.isometricrendersforked.property.PropertyBundle;

/**
 * Renderable that also advances state every client tick.
 *
 * @param <P> property bundle type used by this renderable
 */
public interface TickingRenderable<P extends PropertyBundle> extends Renderable<P> {
    // Advances renderable state by one client tick.
    void tick();
}
