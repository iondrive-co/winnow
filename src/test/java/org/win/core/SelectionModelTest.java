package org.win.core;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class SelectionModelTest {

    @Test
    public void constructor_initializesToFullImage() {
        final SelectionModel model = new SelectionModel(100, 50);

        assertThat(model.getTopLeftX()).isEqualTo(0);
        assertThat(model.getTopLeftY()).isEqualTo(0);
        assertThat(model.getBottomRightX()).isEqualTo(100);
        assertThat(model.getBottomRightY()).isEqualTo(50);
        assertThat(model.getWidth()).isEqualTo(100);
        assertThat(model.getHeight()).isEqualTo(50);
    }

    @Test
    public void setRegion_updatesSelection() {
        final SelectionModel model = new SelectionModel(200, 200);

        model.setRegion(10, 20, 150, 180);

        assertThat(model.getTopLeftX()).isEqualTo(10);
        assertThat(model.getTopLeftY()).isEqualTo(20);
        assertThat(model.getBottomRightX()).isEqualTo(150);
        assertThat(model.getBottomRightY()).isEqualTo(180);
    }

    @Test
    public void setRegion_normalizesInvertedSelection() {
        final SelectionModel model = new SelectionModel(200, 200);

        // Pass bottom-right before top-left
        model.setRegion(150, 180, 10, 20);

        assertThat(model.getTopLeftX()).isEqualTo(10);
        assertThat(model.getTopLeftY()).isEqualTo(20);
        assertThat(model.getBottomRightX()).isEqualTo(150);
        assertThat(model.getBottomRightY()).isEqualTo(180);
    }

    @Test
    public void setRegion_clampsToBounds() {
        final SelectionModel model = new SelectionModel(100, 100);

        model.setRegion(-50, -50, 200, 200);

        assertThat(model.getTopLeftX()).isEqualTo(0);
        assertThat(model.getTopLeftY()).isEqualTo(0);
        assertThat(model.getBottomRightX()).isEqualTo(100);
        assertThat(model.getBottomRightY()).isEqualTo(100);
    }

    @Test
    public void move_shiftsSelection() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(50, 50, 100, 100);

        model.move(10, 20);

        assertThat(model.getTopLeftX()).isEqualTo(60);
        assertThat(model.getTopLeftY()).isEqualTo(70);
        assertThat(model.getBottomRightX()).isEqualTo(110);
        assertThat(model.getBottomRightY()).isEqualTo(120);
        assertThat(model.getWidth()).isEqualTo(50); // Preserved
        assertThat(model.getHeight()).isEqualTo(50); // Preserved
    }

    @Test
    public void move_clampsToBounds() {
        final SelectionModel model = new SelectionModel(100, 100);
        model.setRegion(80, 80, 100, 100);

        // Try to move outside bounds
        model.move(50, 50);

        // Should be clamped to edge
        assertThat(model.getBottomRightX()).isEqualTo(100);
        assertThat(model.getBottomRightY()).isEqualTo(100);
        assertThat(model.getWidth()).isEqualTo(20);
        assertThat(model.getHeight()).isEqualTo(20);
    }

    @Test
    public void expandRight_increasesWidth() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(50, 50, 100, 100);

        model.expandRight(20);

        assertThat(model.getBottomRightX()).isEqualTo(120);
        assertThat(model.getWidth()).isEqualTo(70);
    }

    @Test
    public void expandRight_clampsToImageBound() {
        final SelectionModel model = new SelectionModel(100, 100);
        model.setRegion(50, 50, 90, 90);

        model.expandRight(50);

        assertThat(model.getBottomRightX()).isEqualTo(100);
    }

    @Test
    public void reduceRight_decreasesWidth() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(50, 50, 100, 100);

        model.reduceRight(20);

        assertThat(model.getBottomRightX()).isEqualTo(80);
        assertThat(model.getWidth()).isEqualTo(30);
    }

    @Test
    public void reduceRight_maintainsMinimumWidth() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(50, 50, 60, 60);

        model.reduceRight(100); // Try to reduce more than width

        assertThat(model.getBottomRightX()).isEqualTo(51); // Minimum 1px
    }

    @Test
    public void expandLeft_increasesWidthFromLeft() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(50, 50, 100, 100);

        model.expandLeft(20);

        assertThat(model.getTopLeftX()).isEqualTo(30);
        assertThat(model.getWidth()).isEqualTo(70);
    }

    @Test
    public void reduceLeft_decreasesWidthFromLeft() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(50, 50, 100, 100);

        model.reduceLeft(20);

        assertThat(model.getTopLeftX()).isEqualTo(70);
        assertThat(model.getWidth()).isEqualTo(30);
    }

    @Test
    public void dragTopLeft_movesCorner() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(50, 50, 100, 100);

        model.dragTopLeft(10, 5);

        assertThat(model.getTopLeftX()).isEqualTo(60);
        assertThat(model.getTopLeftY()).isEqualTo(55);
        assertThat(model.getBottomRightX()).isEqualTo(100);
        assertThat(model.getBottomRightY()).isEqualTo(100);
    }

    @Test
    public void dragBottomRight_movesCorner() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(50, 50, 100, 100);

        model.dragBottomRight(15, 25);

        assertThat(model.getTopLeftX()).isEqualTo(50);
        assertThat(model.getTopLeftY()).isEqualTo(50);
        assertThat(model.getBottomRightX()).isEqualTo(115);
        assertThat(model.getBottomRightY()).isEqualTo(125);
    }

    @Test
    public void getVisibleBounds_clampsToViewport() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(0, 0, 200, 200);

        // Viewport only shows part of the image
        final double[] bounds = model.getVisibleBounds(50, 50, 150, 150);

        assertThat(bounds[0]).isEqualTo(50);  // clampedLeft
        assertThat(bounds[1]).isEqualTo(50);  // clampedTop
        assertThat(bounds[2]).isEqualTo(150); // clampedRight
        assertThat(bounds[3]).isEqualTo(150); // clampedBottom
    }

    @Test
    public void getVisibleBounds_returnsFullSelectionWhenFullyVisible() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(50, 50, 100, 100);

        // Viewport larger than selection
        final double[] bounds = model.getVisibleBounds(0, 0, 200, 200);

        assertThat(bounds[0]).isEqualTo(50);
        assertThat(bounds[1]).isEqualTo(50);
        assertThat(bounds[2]).isEqualTo(100);
        assertThat(bounds[3]).isEqualTo(100);
    }

    @Test
    public void resetToFullImage_restoresFullSelection() {
        final SelectionModel model = new SelectionModel(200, 100);
        model.setRegion(50, 25, 100, 75);

        model.resetToFullImage();

        assertThat(model.getTopLeftX()).isEqualTo(0);
        assertThat(model.getTopLeftY()).isEqualTo(0);
        assertThat(model.getBottomRightX()).isEqualTo(200);
        assertThat(model.getBottomRightY()).isEqualTo(100);
    }

    @Test
    public void setImageDimensions_withReset_resetsSelection() {
        final SelectionModel model = new SelectionModel(100, 100);
        model.setRegion(25, 25, 75, 75);

        model.setImageDimensions(200, 150, true);

        assertThat(model.getImageWidth()).isEqualTo(200);
        assertThat(model.getImageHeight()).isEqualTo(150);
        assertThat(model.getWidth()).isEqualTo(200);
        assertThat(model.getHeight()).isEqualTo(150);
    }

    @Test
    public void setImageDimensions_withoutReset_clampsSelection() {
        final SelectionModel model = new SelectionModel(200, 200);
        model.setRegion(50, 50, 180, 180);

        model.setImageDimensions(100, 100, false);

        assertThat(model.getBottomRightX()).isEqualTo(100);
        assertThat(model.getBottomRightY()).isEqualTo(100);
    }

    @Test
    public void onSelectionChange_calledOnModification() {
        final SelectionModel model = new SelectionModel(200, 200);
        final AtomicInteger callCount = new AtomicInteger(0);
        model.setOnSelectionChange(callCount::incrementAndGet);

        model.setRegion(10, 10, 50, 50);
        model.move(5, 5);
        model.expandRight(10);

        assertThat(callCount.get()).isEqualTo(3);
    }

    @Test
    public void getWidthInt_roundsCorrectly() {
        final SelectionModel model = new SelectionModel(100, 100);
        model.setRegion(0, 0, 50.7, 50.3);

        assertThat(model.getWidthInt()).isEqualTo(51);
        assertThat(model.getHeightInt()).isEqualTo(50);
    }
}
