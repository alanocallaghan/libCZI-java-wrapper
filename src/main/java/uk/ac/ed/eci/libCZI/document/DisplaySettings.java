package uk.ac.ed.eci.libCZI.document;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import uk.ac.ed.eci.libCZI.LibCziFFM;


import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class DisplaySettings {

    private final MemorySegment displaySettingsHandle;

    public DisplaySettings(MemorySegment handle) {
        this.displaySettingsHandle = handle;
    }

//    libCZI_DisplaySettingsGetChannelDisplaySettings(DisplaySettingsHandle display_settings_handle, int channel_id, ChannelDisplaySettingsHandle *channel_display_setting)
    public ChannelDisplaySettings getChannelDisplaySettings(int channelIndex) {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);
        MethodHandle getDisplaySettings = LibCziFFM.getMethodHandle("libCZI_DisplaySettingsGetChannelDisplaySettings", descriptor);
        try (var arena = Arena.ofConfined()){
            var channelDisplayHandle = arena.allocate(ADDRESS);
            int errorCode = (int) getDisplaySettings.invokeExact(displaySettingsHandle, channelIndex, channelDisplayHandle);
            if (errorCode != 0) {
                throw new RuntimeException("Failed to get channel display settings. Error code: " + errorCode);
            }
            return new ChannelDisplaySettings(channelDisplayHandle);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call native function libCZI_DisplaySettingsGetChannelDisplaySettings", e);
        }
    }

    public class ChannelDisplaySettings {
        // todo
        // EXTERNALLIBCZIAPI_API(LibCZIApiErrorCode) libCZI_ReleaseChannelDisplaySettings(ChannelDisplaySettingsHandle channel_display_settings_handle);
        private final MemorySegment channelDisplayHandle;

        public ChannelDisplaySettings(MemorySegment channelDisplayHandle) {
            this.channelDisplayHandle = channelDisplayHandle;
        }
    }
}
