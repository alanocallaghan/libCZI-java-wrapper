package uk.ac.ed.eci.libCZI.document;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;


import static java.lang.foreign.ValueLayout.JAVA_INT;

public class AvailableDimensions {

    private final int[] availableDimensionArray;

    private AvailableDimensions(int[] array) {
        this.availableDimensionArray = array;
    }

    @Override
    public String toString() {
        return Arrays.toString(availableDimensionArray);
    }

    public int[] toArray() {
        return availableDimensionArray.clone();
    }

    public static AvailableDimensions createFromMemorySegment(MemorySegment segment) {;
        return new AvailableDimensions(segment.toArray(JAVA_INT));
    }
}
