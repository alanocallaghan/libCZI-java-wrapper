package uk.ac.ed.eci.libCZI;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.*;

/**
 * Represents the dimension bounds of a CZI image.
 * This record corresponds to the `DimBounds` structure in the libCZI C API.
 * It provides information about the valid dimensions, and the start and size
 * for each dimension.
 *
 * @param dimensionsValid The number of valid dimensions.
 * @param start An array containing the start index for each dimension.
 * @param size An array containing the size for each dimension.
 * @see <a href="https://zeiss.github.io/libczi/api/struct_dim_bounds_interop.html">DimBoundsInterop</a>
 * @author Paul Mitchell
 */
public record DimBounds(int dimensionsValid, int[] start, int[] size) {
    public static MemoryLayout layout() {
        return MemoryLayout.structLayout(
                JAVA_INT.withName("dimensions_valid"),
                MemoryLayout.sequenceLayout(LibCziFFM.K_MAX_DIMENSION_COUNT, JAVA_INT).withName("start"),
                MemoryLayout.sequenceLayout(LibCziFFM.K_MAX_DIMENSION_COUNT, JAVA_INT).withName("size"));
    }

    public static DimBounds createFromMemorySegment(MemorySegment segment) {
        MemoryLayout layout = layout();
        int dimensionsValid = segment.get(
                JAVA_INT,
                layout.byteOffset(PathElement.groupElement("dimensions_valid"))
        );

        int[] start = segment
                .asSlice(
                        layout.byteOffset(PathElement.groupElement("start")),
                        layout.select(PathElement.groupElement("start")).byteSize())
                .toArray(JAVA_INT);

        int[] size = segment
                .asSlice(layout.byteOffset(PathElement.groupElement("size")),
                        layout.select(PathElement.groupElement("size")).byteSize())
                .toArray(JAVA_INT);

        return new DimBounds(dimensionsValid, start, size);
    }
}
