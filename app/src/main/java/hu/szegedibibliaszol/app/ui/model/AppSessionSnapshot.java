package hu.szegedibibliaszol.app.ui.model;

import java.util.List;

public record AppSessionSnapshot(
        String translation,
        List<RangeSelectionSnapshot> ranges
) {

    public AppSessionSnapshot {
        ranges = List.copyOf(ranges);
    }

    public boolean isEmpty() {
        return translation == null;
    }
}

