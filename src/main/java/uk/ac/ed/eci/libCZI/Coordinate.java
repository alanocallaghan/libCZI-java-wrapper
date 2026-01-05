package uk.ac.ed.eci.libCZI;

import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
/**
 * This structure gives the coordinates (of a sub-block) for 
 * a set of dimension. The bit at position i in dimensions_valid 
 * indicates whether the coordinate for dimension i+1 is valid. 
 * So, bit 0 is corresponding to dimension 1 (=Z), bit 1 to 
 * dimension 2 (=C), and so on. In the fixed-sized array value, 
 * the coordinate for the dimensions is stored. 
 * The element at position 0 corresponds to the first valid
 * dimension, the element at position 1 to the second valid
 * dimension, and so on. An example would be: 
 * dimensions_valid = 0b00000011, value = { 0, 2 }. 
 * This would mean that the dimension ‘Z’ is valid, 
 * and the coordinate for ‘Z’ is 0, and the dimension ‘C’ is valid, 
 * and the coordinate for ‘C’ is 2.
 * 
 * @author Paul Mitchell
 */

public class Coordinate {
    private int[] value = new int[LibCziFFM.K_MAX_DIMENSION_COUNT];
    private int dimensionsValid;

    public Coordinate(int dimensionsValid, int[] value) {
        this.dimensionsValid = dimensionsValid;
        this.value = value;
    }

    /**
     * Z - 1
     * C - 2
     * T - 4
     * R - 8
     * S - 16
     * I - 32
     * H - 64
     * V - 128
     * B - 256
     */
    public int dimensionsValid() {
        return dimensionsValid;
    }

    public int[] value() {
        return value;
    }

    public static MemoryLayout layout() {
        return MemoryLayout.structLayout(JAVA_INT, MemoryLayout.sequenceLayout(LibCziFFM.K_MAX_DIMENSION_COUNT, JAVA_INT));
    }

    public static Coordinate createC0(int channel) {
        int[] value = new int[LibCziFFM.K_MAX_DIMENSION_COUNT];
        for (int i = 0; i < LibCziFFM.K_MAX_DIMENSION_COUNT; i++) {
            value[i] = 0;
        }
        value[0] = channel;
        return new Coordinate(2, value);
    }

    public MemorySegment toMemorySegment(Arena arena) {
        MemorySegment segment = arena.allocate(layout());
        segment.set(JAVA_INT, 0, dimensionsValid);
        for (int i = 0; i < LibCziFFM.K_MAX_DIMENSION_COUNT; i++) {
            segment.set(JAVA_INT, 4 + i * 4, value[i]);
        }
        return segment;
    }
}
