package uk.ac.ed.eci.libCZI;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import uk.ac.ed.eci.libCZI.bitmaps.Bitmap;
import uk.ac.ed.eci.libCZI.bitmaps.Roi;

public class SingleChannelTileAccessor implements AutoCloseable {
    private final CziStreamReader reader;
    private final MemorySegment accessorHandle;
    private final Arena classArena;
    
    public SingleChannelTileAccessor(CziStreamReader reader) {
        this.reader = reader;
        this.classArena = Arena.ofConfined();
        this.accessorHandle = createAccessor();
    }

    @Override
    public void close() throws Exception {
        free();
        this.classArena.close();
    }

    private MemorySegment createAccessor() {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS);
        MethodHandle createAccessor = LibCziFFM.getMethodHandle("libCZI_CreateSingleChannelTileAccessor", descriptor);
        try {
            MemorySegment pAccessor = classArena.allocate(ADDRESS);

            int errorCode = (int) createAccessor.invokeExact(reader.readerHandle(), pAccessor);
            if (errorCode != 0) {
                throw new CziReaderException("Failed to create single channel tile accessor. Error code: " + errorCode);
            }
            return pAccessor.get(ADDRESS, 0).asReadOnly();
        } catch (Throwable e) {
            if (e instanceof CziReaderException) {
                throw (CziReaderException) e;
            }
            throw new CziReaderException("Failed to call native function libCZI_CreateSingleChannelTileAccessor", e);
        }
    }

    /**
     * Gets the size information of the specified tile accessor based on the region of interest and zoom factor.
     * 
     * @param roi The region of interest that defines the region of interest within the plane for which the size is to be calculated.
     * @param zoom A floating-point value representing the zoom factor. Between 0 and 1
     * @return The size of the tile accessor. It contains width and height information.
     */
    public IntSize calcTileSize(IntRect roi, float zoom) {
        if (zoom < 0 || zoom > 1) {
            throw new IllegalArgumentException("Zoom factor must be between 0 and 1");
        }
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_FLOAT, ADDRESS);
        MethodHandle calcTileSize = LibCziFFM.getMethodHandle("libCZI_SingleChannelTileAccessorCalcSize", descriptor);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pSize = arena.allocate(IntSize.layout());
            MemorySegment pRoi = roi.toMemorySegment(arena);
            int errorCode = (int) calcTileSize.invokeExact(accessorHandle, pRoi, zoom, pSize);
            if (errorCode != 0) {
                throw new CziReaderException("Failed to calculate tile size. Error code: " + errorCode);
            }
            return IntSize.createFromMemorySegment(pSize);
        } catch(Throwable e) {
            if (e instanceof CziReaderException) {
                throw (CziReaderException) e;
            }
            throw new RuntimeException("Failed to call native function libCZI_SingleChannelTileAccessorCalcSize", e);
        }
    }

    public Bitmap getBitmap(Roi roi, float zoom) {
        return getBitmapRaw(roi.toIntRect(), zoom, 0);
    }

    public Bitmap getBitmap(Roi roi, float zoom, int channel) {
        return getBitmapRaw(roi.toIntRect(), zoom, channel);
    }

    public Bitmap getBitmapRaw(IntRect rawRoi, float zoom) {
        return getBitmapRaw(rawRoi, zoom, 0);
    }

    public Bitmap getBitmapRaw(IntRect rawRoi, float zoom, int channel) {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_FLOAT, ADDRESS, ADDRESS);
        MethodHandle getBitmap = LibCziFFM.getMethodHandle("libCZI_SingleChannelTileAccessorGet", descriptor);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pCoordinate = Coordinate.createC0(channel).toMemorySegment(arena);
            MemorySegment pRoi = rawRoi.toMemorySegment(arena);
            MemorySegment pOptions = new AccessorOptions(1,1,1, false, true, null).toMemorySegment(arena);            
            MemorySegment pBitmap = arena.allocate(ADDRESS);
            int errorCode = (int) getBitmap.invokeExact(accessorHandle, pCoordinate, pRoi, zoom, pOptions, pBitmap);
            if (errorCode != 0) {
                throw new CziReaderException("Failed to get bitmap. Error code: " + errorCode);
            }
            MemorySegment bitmapHandle = pBitmap.get(ADDRESS, 0).asReadOnly();
            return new Bitmap(bitmapHandle);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_SingleChannelTileAcessorGet", e);
        }
    }

    private void free() {
        if (accessorHandle == null || accessorHandle.address() == 0) {
            return; // it has already gone!
        }
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS);
        MethodHandle free = LibCziFFM.getMethodHandle("libCZI_ReleaseCreateSingleChannelTileAccessor", descriptor);
        try {
            int errorCode = (int) free.invokeExact(accessorHandle);
            if (errorCode != 0) {
                throw new CziReaderException("Failed to free single channel tile accessor. Error code: " + errorCode);
            }
        } catch (Throwable e) {
            if (e instanceof CziReaderException) {
                throw (CziReaderException) e;
            }
            throw new CziReaderException("Failed to call native function libCZI_ReleaseCreateSingleChannelTileAccessor", e);
        }
    }
}
