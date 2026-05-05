package net.oldmanyounger.isometricrendersforked.util;

/**
 * Supported animation output formats.
 *
 * <p>The values mirror the original Fabric mod's FFmpeg-backed export formats,
 * but the enum is kept independent so global render settings can compile before
 * the full animation exporter is ported.</p>
 */
public enum AnimationFormat {
    APNG("apng", new String[]{"-plays", "0"}),
    GIF("gif", new String[]{"-plays", "0", "-pix_fmt", "yuv420p"}),
    MP4("mp4", new String[]{"-preset", "slow", "-crf", "20", "-pix_fmt", "yuv420p"});

    public final String extension;
    public final String[] arguments;

    /**
     * Creates an animation format description.
     *
     * @param extension the exported file extension
     * @param arguments extra FFmpeg arguments used for this format
     */
    AnimationFormat(String extension, String[] arguments) {
        this.extension = extension;
        this.arguments = arguments;
    }

    // Cycles to the next supported animation format.
    public AnimationFormat next() {
        return switch (this) {
            case MP4 -> APNG;
            case APNG -> GIF;
            case GIF -> MP4;
        };
    }
}
