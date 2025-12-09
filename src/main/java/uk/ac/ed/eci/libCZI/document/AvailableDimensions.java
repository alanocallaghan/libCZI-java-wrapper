package uk.ac.ed.eci.libCZI.document;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;

public class AvailableDimensions {

    private final int[] availableDimensions;

    public AvailableDimensions(int[] out) {
        this.availableDimensions=out;
    }

    @Override
    public String toString() {
        return Arrays.toString(availableDimensions);
    }

    public static AvailableDimensions fromSegment(MemorySegment availableDimensions) {
        return null;
    }
}
