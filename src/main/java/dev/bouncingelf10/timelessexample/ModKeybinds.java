package dev.bouncingelf10.timelessexample;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeybinds {
    public static KeyMapping START_GUI;
    public static KeyMapping PAUSE_GUI;

    public static void register() {
        START_GUI = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.timelesslib.start",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "category." + TimelessExampleMod.MOD_ID
        ));
        PAUSE_GUI = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.timelesslib.pause",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category." + TimelessExampleMod.MOD_ID
        ));
    }
}
