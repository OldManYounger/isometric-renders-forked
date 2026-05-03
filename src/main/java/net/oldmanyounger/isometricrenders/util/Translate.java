package net.oldmanyounger.isometricrenders.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

/**
 * Translation helpers for Isometric Renders messages and GUI labels.
 *
 * <p>The translation keys intentionally keep the original Fabric mod's
 * hyphenated key namespace, such as {@code gui.isometric-renders.export}, even
 * though the NeoForge mod id is {@code isometric_renders}.</p>
 */
public final class Translate {
    public static final Component PREFIX = generatePrefix("Isometric Renders", 190, 155);

    private Translate() {}

    // Creates a translated chat message component.
    public static MutableComponent make(String key, Object... args) {
        return Component.translatable("message.isometric-renders." + key, args);
    }

    // Creates a translated GUI component.
    public static MutableComponent gui(String key, Object... args) {
        return Component.translatable("gui.isometric-renders." + key, args);
    }

    // Creates a prefixed translated chat message.
    public static MutableComponent msg(String key, Object... args) {
        return prefixed(make(key, args).withStyle(ChatFormatting.GRAY));
    }

    // Adds the Isometric Renders prefix to a message.
    public static MutableComponent prefixed(Component text) {
        return Component.empty()
                .append(PREFIX)
                .append(Component.literal(" > ").withStyle(ChatFormatting.DARK_GRAY))
                .append(text);
    }

    // Sends an action bar message to the current client player when available.
    public static void actionBar(String key, Object... args) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(make(key, args), true);
        }
    }

    // Builds the colored Isometric Renders chat prefix.
    private static Component generatePrefix(String text, int startHue, int endHue) {
        int hueSpan = endHue - startHue;
        char[] chars = text.toCharArray();

        var prefixText = Component.empty();

        for (int index = 0; index < chars.length; index++) {
            int currentIndex = index;
            prefixText.append(Component.literal(String.valueOf(chars[index])).withStyle(style ->
                    style.withColor(Mth.hsvToRgb(
                            (startHue + (currentIndex / (float) chars.length) * hueSpan) / 360.0F,
                            1.0F,
                            0.96F
                    ))
            ));
        }

        return prefixText;
    }
}
