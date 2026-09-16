package io.github.waytomee.flexicat.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Key mappings for moving the selected corner. Defaults: arrow keys for
 * up/down/left/right and Page Up / Page Down for away/towards. Left, right, away
 * and towards are relative to the player's horizontal facing; up/down are world Y.
 *
 * <p>WASD was deliberately not used: it would fight with walking around the block
 * while editing. Everything is rebindable in the controls screen.
 */
public final class FlexiCatKeys {

    public static final String CATEGORY = "key.categories.flexicat";

    public static final KeyMapping MOVE_UP = key("move_up", GLFW.GLFW_KEY_UP);
    public static final KeyMapping MOVE_DOWN = key("move_down", GLFW.GLFW_KEY_DOWN);
    public static final KeyMapping MOVE_LEFT = key("move_left", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping MOVE_RIGHT = key("move_right", GLFW.GLFW_KEY_RIGHT);
    public static final KeyMapping MOVE_AWAY = key("move_away", GLFW.GLFW_KEY_PAGE_UP);
    public static final KeyMapping MOVE_TOWARDS = key("move_towards", GLFW.GLFW_KEY_PAGE_DOWN);

    private static final List<KeyMapping> ALL = List.of(
            MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, MOVE_AWAY, MOVE_TOWARDS);

    private FlexiCatKeys() {
    }

    public static List<KeyMapping> all() {
        return ALL;
    }

    private static KeyMapping key(String name, int glfwKey) {
        return new KeyMapping("key.flexicat." + name, InputConstants.Type.KEYSYM, glfwKey, CATEGORY);
    }
}