package net.oldmanyounger.isometricrendersforked.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.oldmanyounger.isometricrendersforked.render.AreaRenderable;
import net.oldmanyounger.isometricrendersforked.screen.RenderScreen;

/**
 * Stores and manages the current client-side area selection.
 *
 * <p>The selection is intentionally simple: press the select key once for the
 * first corner, press it again for the second corner, and sneak while pressing
 * it to clear.</p>
 */
public final class AreaSelectionHelper {
    public static BlockPos pos1 = null;
    public static BlockPos pos2 = null;

    private AreaSelectionHelper() {}

    // Returns whether an area selection is currently in progress.
    public static boolean hasSelectionStart() {
        return pos1 != null;
    }

    // Returns whether both corners have been selected.
    public static boolean hasCompleteSelection() {
        return pos1 != null && pos2 != null;
    }

    // Clears the current selection.
    public static void clear() {
        pos1 = null;
        pos2 = null;
        Translate.actionBar("selection_cleared");
    }

    // Selects the block currently under the crosshair.
    public static void selectTargetedPosition() {
        BlockPos targetPos = targetedBlockPos();

        if (targetPos == null) {
            Translate.actionBar("no_block");
            return;
        }

        if (pos1 == null) {
            pos1 = targetPos;
            pos2 = null;
            Translate.actionBar("selection_started", format(pos1));
            return;
        }

        pos2 = targetPos;
        Translate.actionBar("selection_finished", format(pos1), format(pos2));
    }

    // Opens the selected area in the temporary render screen.
    public static boolean tryOpenScreen() {
        if (!hasCompleteSelection()) {
            return false;
        }

        Minecraft.getInstance().setScreen(new RenderScreen(AreaRenderable.of(pos1, pos2)));
        return true;
    }

    // Returns the currently targeted block position.
    private static BlockPos targetedBlockPos() {
        HitResult hitResult = Minecraft.getInstance().hitResult;

        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            return null;
        }

        if (hitResult instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getBlockPos();
        }

        return BlockPos.containing(hitResult.getLocation());
    }

    // Formats a block position for chat.
    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
