package org.win.core;

/**
 * Shared selection keyboard handling to keep desktop/web shortcut behavior consistent.
 */
public final class SelectionAdjuster {
    private SelectionAdjuster() {}

    /**
     * Apply a selection keyboard shortcut.
     *
     * @param keyCode key code (KeyEvent VK-style for desktop, DOM keyCode for web)
     * @param ctrl    whether Ctrl/Cmd is pressed
     * @param shift   whether Shift is pressed
     * @param delta   number of pixels to adjust by
     * @param model   selection model to modify
     * @return true if handled
     */
    public static boolean applyShortcut(final int keyCode, final boolean ctrl, final boolean shift,
                                        final int delta, final SelectionModel model) {
        if (!ctrl || model == null) {
            return false;
        }
        if (shift) {
            return applyShifted(keyCode, delta, model);
        }
        return applyCtrlOnly(keyCode, delta, model);
    }

    private static boolean applyCtrlOnly(final int keyCode, final int delta, final SelectionModel model) {
        return switch (keyCode) {
            case 68 -> { // D
                model.expandRight(delta);
                yield true;
            }
            case 65 -> { // A
                model.reduceRight(delta);
                yield true;
            }
            case 83 -> { // S
                model.expandBottom(delta);
                yield true;
            }
            case 87 -> { // W
                model.reduceBottom(delta);
                yield true;
            }
            default -> false;
        };
    }

    private static boolean applyShifted(final int keyCode, final int delta, final SelectionModel model) {
        return switch (keyCode) {
            case 68 -> { // D
                model.expandLeft(delta);
                yield true;
            }
            case 65 -> { // A
                model.reduceLeft(delta);
                yield true;
            }
            case 83 -> { // S
                model.expandTop(delta);
                yield true;
            }
            case 87 -> { // W
                model.reduceTop(delta);
                yield true;
            }
            default -> false;
        };
    }
}
