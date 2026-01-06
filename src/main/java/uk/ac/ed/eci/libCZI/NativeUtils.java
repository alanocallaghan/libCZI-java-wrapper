package uk.ac.ed.eci.libCZI;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NativeUtils {

    private static final String RESOURCE_PREFIX = getPrefix();

    private static String getPrefix() {
        return getOS() + "-" + getArch();
    }

    private static String getOS() {
        var name = System.getProperty("os.name").toLowerCase();
        if (name.contains("linux"))
            return "linux";
        if (name.contains("windows"))
            return "windows";
        if (name.contains("darwin") || name.contains("macos"))
            return "darwin";
        throw new IllegalStateException("Unexpected value: " + System.getProperty("os.name").toLowerCase());
    }

    private static String getArch() {
        return switch (System.getProperty("os.arch")) {
            case "aarch64" -> "aarch64";
            case "amd64" -> "x86-64";
            default -> throw new IllegalStateException("Unexpected value: " + System.getProperty("os.arch"));
        };
    }

    public static void loadLibraryFromJar(String libraryFileName) throws IOException {
        // Prepend "native/" to the library file name as it is located in the "native" folder
        String fullLibraryFileName = "native/" + libraryFileName;

        var loader = Thread.currentThread().getContextClassLoader();
        String libname = mapSharedLibraryName(libraryFileName);
        String resourcePath = RESOURCE_PREFIX + "/" + libname;
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        // Prepare a destination file
        Path tempDir = Files.createTempDirectory("native-libs");
        File tempLib = tempDir.resolve(libraryFileName).toFile();
        tempLib.deleteOnExit(); // Delete the file when JVM exits (optional, for cleanup)
        try (InputStream in = loader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Library not found in JAR: " + fullLibraryFileName);
            }
            Files.copy(in, tempLib.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        System.load(tempLib.getAbsolutePath());
    }

    private static boolean isMac() {
        return NativeUtils.getOS().equals("darwin");
    }

    private static boolean isLinux() {
        return NativeUtils.getOS().equals("linux");
    }

    private static boolean isFreeBSD() {
        return false;
    }

    private static boolean isWindows() {
        return NativeUtils.getOS().equals("windows");
    }

    static String mapSharedLibraryName(String libName) {
        if (isMac()) {
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
        else if (isLinux() || isFreeBSD()) {
            if (isVersionedName(libName) || libName.endsWith(".so")) {
                // A specific version was requested - use as is for search
                return libName;
            }
        }
        else if (isWindows()) {
            if (libName.endsWith(".drv") || libName.endsWith(".dll") || libName.endsWith(".ocx")) {
                return libName;
            }
        }
        return System.mapLibraryName(libName);
    }

    private static boolean isVersionedName(String name) {
        if (name.startsWith("lib")) {
            int so = name.lastIndexOf(".so.");
            if (so != -1 && so + 4 < name.length()) {
                for (int i=so+4;i < name.length();i++) {
                    char ch = name.charAt(i);
                    if (!Character.isDigit(ch) && ch != '.') {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

}

