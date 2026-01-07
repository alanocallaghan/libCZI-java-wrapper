package uk.ac.ed.eci.libCZI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


import static uk.ac.ed.eci.libCZI.NativeUtils.Architecture.AARCH64;
import static uk.ac.ed.eci.libCZI.NativeUtils.Architecture.X86_64;
import static uk.ac.ed.eci.libCZI.NativeUtils.OS.FREEBSD;
import static uk.ac.ed.eci.libCZI.NativeUtils.OS.LINUX;
import static uk.ac.ed.eci.libCZI.NativeUtils.OS.MACOS;
import static uk.ac.ed.eci.libCZI.NativeUtils.OS.WINDOWS;


public class NativeUtils {

    private static final String RESOURCE_PREFIX = getPrefix();

    enum OS {
        LINUX,
        FREEBSD,
        WINDOWS,
        MACOS
    }

    enum Architecture {
        AARCH64("aarch64"),
        X86_64("x86-64");
        private String string;

        Architecture(String string) {
            this.string = string;
        }
        @Override
        public String toString() {
            return string;
        }
    }

    private static String getPrefix() {
        return getOS().toString().toLowerCase() + "-" + getArch().toString();
    }

    private static OS getOS() {
        var name = System.getProperty("os.name").toLowerCase();
        if (name.contains("linux"))
            return LINUX;
        if (name.contains("windows"))
            return WINDOWS;
        if (name.contains("darwin") || name.contains("macos"))
            return MACOS;
        throw new IllegalStateException("Unexpected value: " + name);
    }

    private static Architecture getArch() {
        return switch (System.getProperty("os.arch")) {
            case "aarch64" -> AARCH64;
            case "amd64" -> X86_64;
            default -> throw new IllegalStateException("Unexpected value: " + System.getProperty("os.arch"));
        };
    }

    public static void loadLibraryFromJar(String libraryFileName) throws IOException {
//        var loader = Thread.currentThread().getContextClassLoader();
        var loader = NativeUtils.class.getClassLoader();
        String libname = mapSharedLibraryName(libraryFileName);
        String resourcePath = RESOURCE_PREFIX + "/" + libname;
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        // Prepare a destination file
        Path tempDir = Files.createTempDirectory("native-libs");
        File tempLib = tempDir.resolve(libname).toFile();
        tempLib.deleteOnExit(); // Delete the file when JVM exits (optional, for cleanup)
        try (InputStream in = loader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Library not found in JAR: " + libname);
            }
            Files.copy(in, tempLib.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        System.load(tempLib.getAbsolutePath());
    }

    static String mapSharedLibraryName(String libName) {
        if (getOS() == MACOS) {
            if (libName.startsWith("lib")
                    && (libName.endsWith(".dylib")
                    || libName.endsWith(".jnilib"))) {
                return libName;
            }
            String name = System.mapLibraryName(libName);
            // On MacOSX, System.mapLibraryName() returns the .jnilib extension
            // (the suffix for JNI libraries); ordinarily shared libraries have
            // a .dylib suffix
            if (name.endsWith(".jnilib")) {
                return name.substring(0, name.lastIndexOf(".jnilib")) + ".dylib";
            }
            return name;
        }
        return System.mapLibraryName(libName);
    }

}

