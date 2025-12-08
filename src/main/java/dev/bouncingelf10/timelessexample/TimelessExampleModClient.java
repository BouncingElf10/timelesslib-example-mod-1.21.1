package dev.bouncingelf10.timelessexample;

import dev.bouncingelf10.timelesslib.TimelessLibClient;
import dev.bouncingelf10.timelesslib.api.time.Duration;
import dev.bouncingelf10.timelesslib.api.time.TimeConversions;
import dev.bouncingelf10.timelesslib.api.time.TimeFormatter;
import dev.bouncingelf10.timelesslib.api.time.TimeParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class TimelessExampleModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TimelessExampleMod.LOGGER.info("TimelessExampleModClient Initializing");

        ModKeybinds.register();
        HudRenderCallback.EVENT.register(new DemoHudOverlay());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ModKeybinds.START_GUI.consumeClick()) {
                TimelessExampleMod.LOGGER.info("Starting GUI");
                DemoHudOverlay.playOrReset();
            }
            if (ModKeybinds.PAUSE_GUI.consumeClick()) {
                TimelessExampleMod.LOGGER.info("Pausing GUI");
                DemoHudOverlay.pauseOrUnpause();
            }
        });
        ClientReceiveMessageEvents.CHAT.register((component, chatMessage, gameProfile, bound, timeAtMessage)-> {
            String text = component.toFlatList().getLast().getString();
            try {
                Duration time = TimeParser.parse(text);
                DemoHudOverlay.chatParsedTimeString = time.toString(TimeFormatter.TimeFormat.DIGITAL_MILLIS);
            } catch (Exception e) {
                DemoHudOverlay.chatParsedTimeString = e.getMessage();
            }
        });
    }
}