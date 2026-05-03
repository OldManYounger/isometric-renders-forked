package net.oldmanyounger.isometricrenders.util;

import net.minecraft.world.phys.AABB;
import net.oldmanyounger.isometricrenders.IsometricRenders;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Describes which particles may be rendered while an object is being captured.
 *
 * <p>This is used by particle-related mixins later in the port. The class is
 * safe to add now because it has no mixin dependency by itself.</p>
 *
 * @param <T> condition type used by the selected allow mode
 */
public class ParticleRestriction<T> {
    public static final Allow<Supplier<Boolean>> ALLOW_DURING_TICK = new Allow<>();
    public static final Allow<Void> ALLOW_ALWAYS = new Allow<>();
    public static final Allow<Void> ALLOW_NEVER = new Allow<>();
    public static final Allow<Predicate<AABB>> ALLOW_IN_AREA = new Allow<>();

    private static final ParticleRestriction<Supplier<Boolean>> DURING_TICK =
            new ParticleRestriction<>(ALLOW_DURING_TICK, () -> IsometricRenders.inRenderableTick);
    private static final ParticleRestriction<Void> ALWAYS =
            new ParticleRestriction<>(ALLOW_ALWAYS, null);
    private static final ParticleRestriction<Void> NEVER =
            new ParticleRestriction<>(ALLOW_NEVER, null);

    private final Allow<T> allow;
    private final T condition;

    /**
     * Creates a particle restriction.
     *
     * @param allow the allow mode
     * @param condition optional condition data for the allow mode
     */
    private ParticleRestriction(Allow<T> allow, T condition) {
        this.allow = allow;
        this.condition = condition;
    }

    // Allows particles only while renderable ticking is active.
    public static ParticleRestriction<Supplier<Boolean>> duringTick() {
        return DURING_TICK;
    }

    // Always allows particles.
    public static ParticleRestriction<Void> always() {
        return ALWAYS;
    }

    // Never allows particles.
    public static ParticleRestriction<Void> never() {
        return NEVER;
    }

    // Allows particles inside a specific area.
    public static ParticleRestriction<Predicate<AABB>> inArea(AABB area) {
        return new ParticleRestriction<>(ALLOW_IN_AREA, area::intersects);
    }

    // Checks whether this restriction uses the supplied allow mode.
    public boolean is(Allow<?> allow) {
        return this.allow == allow;
    }

    // Returns this restriction's condition for the supplied allow mode.
    @SuppressWarnings("unchecked")
    public <C> C conditionFor(Allow<C> allow) {
        return (C) this.condition;
    }

    /**
     * Marker object for a particle allow mode.
     *
     * @param <D> condition type associated with this allow mode
     */
    public static class Allow<D> {
        private Allow() {}
    }
}
