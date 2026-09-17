package io.github.waytomee.flexicat.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Key mappings for the corner tool. Defaults: arrow keys for up/down/left/right and
 * Page Up / Page Down for away/towards (left, right, away and towards are relative to
 * the player's horizontal facing; up/down are world Y); Home copies the aimed block's
 * shape onto the tool, End pastes it.
 *
 * <p>WASD was deliberately not used: it would fight with walking around the block
 * while editing. Copy/paste avoid C/V for the same reason (chat, sneak rebinds) and
 * because the tool only reacts to them while it is held, so they never clash with
 * anything else. Everything is rebindable in the controls screen.
 */
public final class FlexiCatKeys {

    public static final String CATEGORY = "key.categories.flexicat";

    public static final KeyMapping MOVE_UP = key("move_up", GLFW.GLFW_KEY_UP);
    public static final KeyMapping MOVE_DOWN = key("move_down", GLFW.GLFW_KEY_DOWN);
    public static final KeyMapping MOVE_LEFT = key("move_left", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping MOVE_RIGHT = key("move_right", GLFW.GLFW_KEY_RIGHT);
    public static final KeyMapping MOVE_AWAY = key("move_away", GLFW.GLFW_KEY_PAGE_UP);
    public static final KeyMapping MOVE_TOWARDS = key("move_towards", GLFW.GLFW_KEY_PAGE_DOWN);
    public static final KeyMapping COPY_SHAPE = key("copy_shape", GLFW.GLFW_KEY_HOME);
    public static final KeyMapping PASTE_SHAPE = key("paste_shape", GLFW.GLFW_KEY_END);

    private static final List<KeyMapping> MOVES = List.of(
            MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, MOVE_AWAY, MOVE_TOWARDS);
    private static final List<KeyMapping> ALL = List.of(
            MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, MOVE_AWAY, MOVE_TOWARDS, COPY_SHAPE, PASTE_SHAPE);

    private FlexiCatKeys() {
    }

    /** The six corner-move keys, in a fixed order (repeater slots). */
    public static List<KeyMapping> moves() {
        return MOVES;
    }

    /** Every key mapping to register. */
    public static List<KeyMapping> all() {
        return ALL;
    }

    private static KeyMapping key(String name, int glfwKey) {
        return new KeyMapping("key.flexicat." + name, InputConstants.Type.KEYSYM, glfwKey, CATEGORY);
    }
}