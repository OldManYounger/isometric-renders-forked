package net.oldmanyounger.isometricrenders.property;

import net.oldmanyounger.isometricrenders.util.AnimationFormat;

/**
 * Shared render and export settings.
 *
 * <p>These values are intentionally static to match the original mod's global
 * render-control behavior. UI controls and commands will read and update these
 * properties once those systems are ported.</p>
 */
public final class GlobalProperties {
    // ==================================
    //  RENDER OPTIONS
    // ==================================

    public static int backgroundColor = 0x000000;

    // ==================================
    //  EXPORT OPTIONS
    // ==================================

    public static final Property<Boolean> unsafe = Property.of(false);
    public static final Property<Boolean> saveIntoRoot = Property.of(true);
    public static final Property<Boolean> overwriteLatest = Property.of(false);

    public static int exportResolution = 1000;

    // ==================================
    //  ANIMATION OPTIONS
    // ==================================

    public static int exportFramerate = 30;
    public static int exportFrames = 60;

    public static AnimationFormat animationFormat = AnimationFormat.APNG;

    private GlobalProperties() {}
}
