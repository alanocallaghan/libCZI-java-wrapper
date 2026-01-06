package uk.ac.ed.eci.libCZI;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.ByteOrder;
import java.util.UUID;

import uk.ac.ed.eci.libCZI.metadata.BuildInformation;
import uk.ac.ed.eci.libCZI.metadata.LibraryVersion;

import static java.lang.foreign.ValueLayout.*;


import java.io.IOException;

public class LibCziFFM {
    public static final Arena GLOBAL_ARENA = Arena.ofAuto();
    public static final SymbolLookup SYMBOL_LOOKUP = getSymbolLookup();
    public static final int K_MAX_DIMENSION_COUNT = 9;
    public static final int K_MIN_DIMENSION_COUNT = 1;

    private static SymbolLookup getSymbolLookup() {
        String libName = System.mapLibraryName("libCZIAPI");
        try {
            NativeUtils.loadLibraryFromJar(libName);
            return SymbolLookup.loaderLookup();
        } catch (IOException e) {
            throw new UnsatisfiedLinkError("Count not load CZI library from JAR: " + e.getMessage());
        }
    }

    public static MethodHandle getMethodHandle(final String methodName, FunctionDescriptor descriptor) {
        return Linker
                .nativeLinker()
                .downcallHandle(
                        SYMBOL_LOOKUP.find(methodName).orElseThrow(
                                () -> new UnsatisfiedLinkError("Could not find symbol: " + methodName)),
                        descriptor);
    }

    // While the node and clock sequence fields are usually stored byte-for-byte
    // in network byte order (big-endian), the first three fields
    // (time_low, time_mid, time_hi_and_version) are often stored in little-endian
    // byte order within the 16-byte array, especially on Windows or when
    // dealing with Intel-based systems. Java's UUID construct expects
    // both long values to be in big-endian (network) order
    //
    public static UUID guidToUuidConvert(MemorySegment guidSegment) {
        byte[] guidBytes = new byte[16];
        guidSegment.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN).get(guidBytes);

        // Then manually reorder the first 8 bytes to get the mostSigBits
        // For a typical Windows GUID (time_low, time_mid, time_hi_and_version are
        // little-endian)
        long msb = 0;
        msb |= ((long) guidBytes[3] & 0xFF) << 56; // time_low (byte 3)
        msb |= ((long) guidBytes[2] & 0xFF) << 48; // time_low (byte 2)
        msb |= ((long) guidBytes[1] & 0xFF) << 40; // time_low (byte 1)
        msb |= ((long) guidBytes[0] & 0xFF) << 32; // time_low (byte 0)

        msb |= ((long) guidBytes[5] & 0xFF) << 24; // time_mid (byte 1)
        msb |= ((long) guidBytes[4] & 0xFF) << 16; // time_mid (byte 0)

        msb |= ((long) guidBytes[7] & 0xFF) << 8; // time_hi_and_version (byte 1)
        msb |= ((long) guidBytes[6] & 0xFF); // time_hi_and_version (byte 0)

        // The last 8 bytes (clock_seq and node) are typically big-endian.
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            lsb = (lsb << 8) | (guidBytes[8 + i] & 0xFF);
        }

        return new UUID(msb, lsb);
    }

    public static MemorySegment allocate(long size) {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS);
        MethodHandle allocate = getMethodHandle("libCZI_AllocateMemory", descriptor);
        MemorySegment pointer = GLOBAL_ARENA.allocate(ADDRESS);
        try {
            int errorCode = (int) allocate.invokeExact(size, pointer);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to allocate memory. Error code: " + errorCode);
            }
            return pointer.get(ValueLayout.ADDRESS, 0).reinterpret(size); //Derefed
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_AllocateMemory", e);
        }
    }

    public static void free(MemorySegment segment) {
        if (segment == null || segment.address() == 0) {
            return;
        }
        FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(ADDRESS);
        MethodHandle free = getMethodHandle("libCZI_Free", descriptor);
        try {
            free.invokeExact(segment);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_Free", e);
        }
    }

    public static LibraryVersion getLibraryVersion() {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS);
        MethodHandle getLibraryVersion = getMethodHandle("libCZI_GetLibCZIVersionInfo", descriptor);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pVersion = arena.allocate(LibraryVersion.LAYOUT);
            int errorCode = (int) getLibraryVersion.invokeExact(pVersion);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to get library version. Error code: " + errorCode);
            }
            return LibraryVersion.fromMemorySegment(pVersion);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_GetLibCZIVersionInfo", e);
        }            
    }

    public static BuildInformation getLibraryBuildInformation() {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS);
        MethodHandle getLibraryVersion = getMethodHandle("libCZI_GetLibCZIBuildInformation", descriptor);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pBuild = arena.allocate(BuildInformation.LAYOUT);
            int errorCode = (int) getLibraryVersion.invokeExact(pBuild);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to get library version. Error code: " + errorCode);
            }
            return BuildInformation.fromMemorySegment(pBuild);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_GetLibCZIVersionInfo", e);
        }         
    }
}