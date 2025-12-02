package com.z.c.woodexcess_api.util;


public final class FileUtils {

    private FileUtils() {
        throw new UnsupportedOperationException("Utility class");
    }


    public static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
