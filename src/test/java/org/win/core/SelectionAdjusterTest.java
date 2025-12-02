package org.win.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SelectionAdjusterTest {

    @Test
    public void ctrlShortcutsAdjustBottomRight() {
        final SelectionModel model = new SelectionModel(100, 100);
        model.setRegion(10, 10, 20, 20);

        assertThat(SelectionAdjuster.applyShortcut(68, true, false, 5, model)).isTrue(); // D
        assertThat(model.getBottomRightX()).isEqualTo(25);

        assertThat(SelectionAdjuster.applyShortcut(65, true, false, 5, model)).isTrue(); // A
        assertThat(model.getBottomRightX()).isEqualTo(20);

        assertThat(SelectionAdjuster.applyShortcut(83, true, false, 5, model)).isTrue(); // S
        assertThat(model.getBottomRightY()).isEqualTo(25);

        assertThat(SelectionAdjuster.applyShortcut(87, true, false, 5, model)).isTrue(); // W
        assertThat(model.getBottomRightY()).isEqualTo(20);
    }

    @Test
    public void ctrlShiftShortcutsAdjustTopLeft() {
        final SelectionModel model = new SelectionModel(100, 100);
        model.setRegion(10, 10, 20, 20);

        assertThat(SelectionAdjuster.applyShortcut(68, true, true, 5, model)).isTrue(); // D expands left (moves x left)
        assertThat(model.getTopLeftX()).isEqualTo(5);

        assertThat(SelectionAdjuster.applyShortcut(65, true, true, 5, model)).isTrue(); // A reduces left (moves x right)
        assertThat(model.getTopLeftX()).isEqualTo(10);

        assertThat(SelectionAdjuster.applyShortcut(83, true, true, 5, model)).isTrue(); // S
        assertThat(model.getTopLeftY()).isEqualTo(5);

        assertThat(SelectionAdjuster.applyShortcut(87, true, true, 5, model)).isTrue(); // W
        assertThat(model.getTopLeftY()).isEqualTo(10);
    }

    @Test
    public void nonCtrlIsIgnored() {
        final SelectionModel model = new SelectionModel(50, 50);
        model.setRegion(5, 5, 10, 10);

        assertThat(SelectionAdjuster.applyShortcut(68, false, false, 5, model)).isFalse();
        assertThat(model.getBottomRightX()).isEqualTo(10);
    }
}
