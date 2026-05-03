package net.oldmanyounger.isometricrenders;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Main NeoForge entry point for the Isometric Renders port.
 *
 * <p>This class intentionally stays small while the Fabric codebase is ported in
 * milestones. Client-only Minecraft classes should be registered through
 * {@link IsometricRendersForkedClient} instead of being loaded directly here.</p>
 */
@Mod(IsometricRendersForked.MOD_ID)
public final class IsometricRendersForked {
    public static final String MOD_ID = "isometric_renders_forked";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static boolean inRenderableDraw = false;
    public static boolean inRenderableTick = false;

    // Registers the mod on NeoForge's mod event bus.
    public IsometricRendersForked(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Loading Isometric Renders NeoForge port");

        // Keep client-only setup isolated from the common mod entry point.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            IsometricRendersForkedClient.register(modEventBus);
        }
    }

    // Marks the start of renderable draw work.
    public static void beginRenderableDraw() {
        inRenderableDraw = true;
    }

    // Marks the end of renderable draw work.
    public static void endRenderableDraw() {
        inRenderableDraw = false;
    }

    // Marks the start of renderable tick work.
    public static void beginRenderableTick() {
        inRenderableTick = true;
    }

    // Marks the end of renderable tick work.
    public static void endRenderableTick() {
        inRenderableTick = false;
    }
}
