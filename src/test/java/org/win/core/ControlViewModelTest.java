package org.win.core;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class ControlViewModelTest {

    @Test
    public void setFilenameNotifiesOncePerChange() {
        final ControlViewModel viewModel = new ControlViewModel();
        final AtomicInteger renameCalls = new AtomicInteger();
        viewModel.bindRename(name -> renameCalls.incrementAndGet());

        viewModel.setFilename("first");
        viewModel.setFilename("first");

        assertThat(renameCalls).hasValue(1);
        assertThat(viewModel.filename()).isEqualTo("first");
    }

    @Test
    public void syncFilenameUpdatesWithoutCallback() {
        final ControlViewModel viewModel = new ControlViewModel();
        final AtomicInteger renameCalls = new AtomicInteger();
        viewModel.bindRename(name -> renameCalls.incrementAndGet());

        viewModel.setFilename("original");
        viewModel.syncFilename("from-navigation");

        assertThat(viewModel.filename()).isEqualTo("from-navigation");
        assertThat(renameCalls).hasValue(1);
    }
}
