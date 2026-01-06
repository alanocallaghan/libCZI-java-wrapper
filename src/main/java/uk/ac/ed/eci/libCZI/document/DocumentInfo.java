package uk.ac.ed.eci.libCZI.document;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import java.nio.charset.StandardCharsets;
import uk.ac.ed.eci.libCZI.LibCziFFM;

public class DocumentInfo {
    private MemorySegment cziDocumentHandle;
    private final Arena classArena;

    public DocumentInfo(MemorySegment readerHandle) {
        this.classArena = Arena.ofConfined();
        this.cziDocumentHandle = getCziDocumentHandle(readerHandle);
    }

    public void close() throws Exception {
        releaseDocumentInfo();
        this.classArena.close();
    }
    
    //libCZI_CziDocumentInfoGetGeneralDocumentInfo
    public GeneralDocumentInfo generalDocumentInfo() {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS);
        MethodHandle getGeneralDocumentInfo = LibCziFFM.getMethodHandle("libCZI_CziDocumentInfoGetGeneralDocumentInfo", descriptor);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pGeneralDocumentInfo = arena.allocate(ADDRESS);
            int errorCode = (int) getGeneralDocumentInfo.invokeExact(cziDocumentHandle, pGeneralDocumentInfo);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to get general document info. Error code: " + errorCode);
            }
            MemorySegment pString = pGeneralDocumentInfo.get(ADDRESS, 0);
            String strJson = pString.reinterpret(Long.MAX_VALUE).getString(0);
            LibCziFFM.free(pString);

            return GeneralDocumentInfo.fromJson(strJson);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_CziDocumentInfoGetGeneralDocumentInfo", e);
        }
    }
    
    //libCZI_CziDocumentInfoGetScalingInfo
    public ScalingInfo scalingInfo() {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS);
        MethodHandle getScalingInfo = LibCziFFM.getMethodHandle("libCZI_CziDocumentInfoGetScalingInfo", descriptor);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pScalingInfo = arena.allocate(ScalingInfo.LAYOUT);
            int errorCode = (int) getScalingInfo.invokeExact(cziDocumentHandle, pScalingInfo);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to get scaling info. Error code: " + errorCode);
            }
            return ScalingInfo.createFromMemorySegment(pScalingInfo);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_CziDocumentInfoGetScalingInfo", e);
        }
    }
    
    //libCZI_CziDocumentInfoGetAvailableDimension
    public AvailableDimensions availableDimensions() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    //libCZI_CziDocumentInfoGetDisplaySettings
    public DisplaySettings displaySettings() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
    //libCZI_CziDocumentInfoGetDimensionInfo
    public DimensionInfo dimensionInfo(int dimension) {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);
        MethodHandle getAvailableDimension = LibCziFFM.getMethodHandle("libCZI_CziDocumentInfoGetDimensionInfo", descriptor);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dataPP = arena.allocate(ADDRESS);
            int errorCode = (int) getAvailableDimension.invokeExact(cziDocumentHandle, dimension, dataPP);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to get dimension info. Error code: " + errorCode);
            }
            MemorySegment dataP = dataPP.get(ADDRESS, 0);
            MemorySegment stringSeg = dataP.reinterpret(20000, arena,
                    segment -> {
                        try {
                            LibCziFFM.free(segment);
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    });
            String s = stringSeg.getString(0, StandardCharsets.UTF_8);
            return new DimensionInfo(s);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_CziDocumentInfoGetAvailableDimension", e);
        }
    }


    private MemorySegment getCziDocumentHandle(MemorySegment handle) {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS);
        MethodHandle getDocumentInfo = LibCziFFM.getMethodHandle("libCZI_MetadataSegmentGetCziDocumentInfo", descriptor);
        try {
            MemorySegment pDocumentInfo = classArena.allocate(ADDRESS);
            int errorCode = (int) getDocumentInfo.invokeExact(handle, pDocumentInfo);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to get CZI document info. Error code: " + errorCode);
            }
            return pDocumentInfo.get(ADDRESS, 0).asReadOnly();
        }
        catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_MetadataSegmentGetCziDocumentInfo", e);
        }
    }

    private void releaseDocumentInfo() {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS);
        MethodHandle release = LibCziFFM.getMethodHandle("libCZI_ReleaseCziDocumentInfo", descriptor);
        try {
            int errorCode = (int) release.invokeExact(cziDocumentHandle);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to release CZI document info. Error code: " + errorCode);
            }
        }
        catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_ReleaseCziDocumentInfo", e);
        }
    }
}
