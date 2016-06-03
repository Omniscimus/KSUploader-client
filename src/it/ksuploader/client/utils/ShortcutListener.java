package it.ksuploader.client.utils;

import it.ksuploader.client.Configuration.Setting;
import it.ksuploader.client.KSUploader;

import java.util.ArrayList;
import java.util.Arrays;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

/**
 * Used for listening to keyboard events. Can detect when the user uses a
 * keyboard shortcut.
 */
public class ShortcutListener implements NativeKeyListener {

    private final ArrayList<Integer> pressedKeys;
    private Shortcut[] shortcuts;

    /**
     * Constructs a new ShortcutListener.
     */
    public ShortcutListener() {
        pressedKeys = new ArrayList<>();
    }

    /**
     * Starts listening for shortcut combinations on the keyboard.
     *
     * @throws NativeHookException If the Operating System does not allow
     * listening.
     */
    public void enable() throws NativeHookException {
        shortcuts = getShortcuts();
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(this);
    }

    /**
     * Stops listening for shortcut combinations on the keyboard.
     */
    public void disable() {
        pressedKeys.clear();
        GlobalScreen.removeNativeKeyListener(this);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        pressedKeys.add(e.getKeyCode());
        checkForShortcuts();
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        pressedKeys.remove(Integer.valueOf(e.getKeyCode()));
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        /* Called when a typed Unicode character is sent to the system by the
         keyboard. */
    }

    /**
     * Gets the shortcuts from the Settings.
     *
     * @return an array of active Shortcuts
     */
    private Shortcut[] getShortcuts() {
        return new Shortcut[]{
            Shortcut.fromSetting(Setting.SHORTCUT_CLIPBOARD),
            Shortcut.fromSetting(Setting.SHORTCUT_COMPLETE_SCREENSHOT),
            Shortcut.fromSetting(Setting.SHORTCUT_FILE),
            Shortcut.fromSetting(Setting.SHORTCUT_PARTIAL_SCREENSHOT)};
    }

    /**
     * Checks if there are any shortcuts triggered by the keys that are
     * currently being pressed.
     *
     * @return true if a shortcut was triggered; false otherwise
     */
    private boolean checkForShortcuts() {
        /* Don't allow 1-key shortcuts to save performance. */
        if (pressedKeys.size() < 2) {
            return false;
        }
        for (Shortcut shortcut : shortcuts) {
            if (shortcut.isTriggered(pressedKeys.stream().mapToInt(i -> i).toArray())) {
                shortcut.run();
                /* Break, because there can't be multiple shortcuts with the
                 same keys. */
                return true;
            }
        }
        return false;
    }

}

/**
 * Represents a keyboard shortcut which the user can use to quickly access a
 * function in this program.
 */
class Shortcut implements Runnable {

    private final int[] keys;
    private final Runnable action;

    /**
     * Creates a new Shortcut.
     *
     * @param keys The keycodes of the keys that need to be pressed in order to
     * trigger this shortcut.
     * @param action The action that should be performed when this shortcut is
     * triggered.
     */
    public Shortcut(int[] keys, Runnable action) {
        Arrays.sort(keys);
        this.keys = keys;
        this.action = action;
    }

    /**
     * Creates a Shortcut using the shortcut keys and the action that belong to
     * the Setting.
     *
     * @param setting The Setting from which to construct the Shortcut.
     */
    public static Shortcut fromSetting(Setting setting) {
        switch (setting) {
            case SHORTCUT_CLIPBOARD:
                return new Shortcut((int[]) setting.getValue(), () -> {
                    KSUploader.inst.getSystemTrayMenu().uploadClipboard();
                });
            case SHORTCUT_COMPLETE_SCREENSHOT:
                return new Shortcut((int[]) setting.getValue(), () -> {
                    KSUploader.inst.getSystemTrayMenu().uploadCompleteScreen();
                });
            case SHORTCUT_FILE:
                return new Shortcut((int[]) setting.getValue(), () -> {
                    KSUploader.inst.getSystemTrayMenu().uploadFile();
                });
            case SHORTCUT_PARTIAL_SCREENSHOT:
                return new Shortcut((int[]) setting.getValue(), () -> {
                    KSUploader.inst.getSystemTrayMenu().uploadPartialScreen();
                });
            default:
                throw new UnsupportedOperationException("Shortcut setting not recognized: " + setting.toString());
        }
    }

    /**
     * Performs the action that should be done when this Shortcut is triggered.
     */
    @Override
    public void run() {
        action.run();
    }

    /**
     * Gets whether this shortcut is triggered by the given array of keys.
     *
     * @param keys An array of keycodes against which this shortcut should be
     * checked.
     * @return true if and only if {@code keys} has the same length and contains
     * the same values as the array of keys for this Shortcut
     */
    public boolean isTriggered(int[] keys) {
        if (keys.length != this.keys.length) {
            return false;
        }
        Arrays.sort(keys);
        return Arrays.equals(keys, this.keys);
    }

}
