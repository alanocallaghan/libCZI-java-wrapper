package uk.ac.ed.eci.libCZI.document;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static uk.ac.ed.eci.libCZI.LibCziFFM.free;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import java.nio.charset.StandardCharsets;
import uk.ac.ed.eci.libCZI.LibCziFFM;

public class DocumentInfo {
    private final MemorySegment cziDocumentHandle;
    private final Arena classArena;
    private MemorySegment displaySettingsHandle;

    public DocumentInfo(MemorySegment readerHandle) {
        this.classArena = Arena.ofConfined();
        this.cziDocumentHandle = getCziDocumentHandle(readerHandle);
    }

    public void close() throws Exception {
        releaseDocumentInfo();
        this.classArena.close();
        LibCziFFM.free(displaySettingsHandle);
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
            free(pString);

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
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);
        MethodHandle getAvailableDimension = LibCziFFM.getMethodHandle("libCZI_CziDocumentInfoGetAvailableDimension", descriptor);
        try (Arena arena = Arena.ofConfined()) {
            var availableDimensionsCount = LibCziFFM.K_MAX_DIMENSION_COUNT + 1;
            var array = arena.allocate(JAVA_INT, availableDimensionsCount);
            int errorCode = (int) getAvailableDimension.invokeExact(cziDocumentHandle, availableDimensionsCount, array);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to get available dimensions. Error code: " + errorCode);
            }
            return AvailableDimensions.createFromMemorySegment(array);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_CziDocumentInfoGetAvailableDimension", e);
        }
    }

    //libCZI_CziDocumentInfoGetDisplaySettings
    public DisplaySettings displaySettings() {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS);
        MethodHandle getDisplaySettings = LibCziFFM.getMethodHandle("libCZI_CziDocumentInfoGetDisplaySettings", descriptor);
        try {
            displaySettingsHandle = classArena.allocate(ADDRESS);
            int errorCode = (int) getDisplaySettings.invokeExact(cziDocumentHandle, displaySettingsHandle);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to get display settings. Error code: " + errorCode);
            }
            return new DisplaySettings(displaySettingsHandle.get(ADDRESS, 0).asReadOnly());
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_CziDocumentInfoGetDisplaySettings", e);
        }

    }


    //libCZI_CziDocumentInfoGetDimensionInfo
    public DimensionInfo dimensionInfo(int dimensionIndex) { //todo passing arg here doesn't work even though hard-coding below does
        // EXTERNALLIBCZIAPI_API(LibCZIApiErrorCode) libCZI_CziDocumentInfoGetDimensionInfo(CziDocumentInfoHandle czi_document_info, std::uint32_t dimension_index, void** dimension_info_json);
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);
        MethodHandle getAvailableDimension = LibCziFFM.getMethodHandle("libCZI_CziDocumentInfoGetDimensionInfo", descriptor);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outPtr = arena.allocate(ADDRESS);
            // todo explain why 0 segfault, 1 throws error (retval 1, invalid arg), 2 channels, 3 time/Z?, 4 segfault, 5 segfault...
            // 3: {"start_time":"2021-06-30T10:40:27Z"}
            int errorCode = (int) getAvailableDimension.invokeExact(cziDocumentHandle, 2, outPtr);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to get available dimensions. Error code: " + errorCode);
            }
            MemorySegment ptr = outPtr.get(ADDRESS, 0);
            // todo sensible byte size somehow
            MemorySegment mptr = ptr.reinterpret(20000, arena, s -> {
                    try {
                        LibCziFFM.free(s);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
            String s = mptr.getString(0, StandardCharsets.UTF_8);
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
