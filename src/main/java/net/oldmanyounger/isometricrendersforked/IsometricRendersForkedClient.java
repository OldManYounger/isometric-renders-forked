package net.oldmanyounger.isometricrendersforked;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.oldmanyounger.isometricrendersforked.command.IsorenderCommand;
import net.oldmanyounger.isometricrendersforked.util.AreaSelectionHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only bootstrap for the NeoForge port.
 *
 * <p>Rendering, screens, commands, HUD overlays, and key handling should be
 * reintroduced here or through client-only helper classes as each subsystem is
 * ported.</p>
 */
public final class IsometricRendersForkedClient {
    public static final KeyMapping SELECT_AREA = new KeyMapping(
            "key.isometric-renders-forked.area_select",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.isometric-renders-forked"
    );

    private IsometricRendersForkedClient() {}

    // Registers client lifecycle listeners on the mod event bus.
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(IsometricRendersForkedClient::onClientSetup);
        modEventBus.addListener(IsometricRendersForkedClient::registerKeyMappings);

        // Client commands are registered on NeoForge's main event bus.
        NeoForge.EVENT_BUS.addListener(IsorenderCommand::register);
        NeoForge.EVENT_BUS.addListener(IsometricRendersForkedClient::onClientTick);
    }


    // Runs once during NeoForge client setup.
    private static void onClientSetup(FMLClientSetupEvent event) {
        IsometricRendersForked.LOGGER.info("Isometric Renders client setup complete");
    }

    // Registers client key mappings.
    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SELECT_AREA);
    }

    // Handles area-selection key presses after each client tick.
    private static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (SELECT_AREA.consumeClick()) {
            if (Screen.hasShiftDown() || minecraft.player.isShiftKeyDown()) {
                AreaSelectionHelper.clear();
            } else {
                AreaSelectionHelper.selectTargetedPosition();
            }
        }
    }
}
