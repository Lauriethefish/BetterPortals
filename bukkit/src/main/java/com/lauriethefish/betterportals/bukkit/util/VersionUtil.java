package com.lauriethefish.betterportals.bukkit.util;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Various parts of the plugin need different implementations based on the minecraft version.
 */
public class VersionUtil {
    // Cache since it's not super cheap to check if we're above a version
    private static final ConcurrentMap<String, Boolean> versionAtLeastCache = new ConcurrentHashMap<>();

    public static String getCurrentVersion() {
        return Bukkit.getServer().getBukkitVersion();
    }

    /**
     * Finds if the version represented by <code>versionStr</code> is greater than or equal to <code>otherStr</code>/
     * E.g. this would return false for "1.8.9" and "1.9.0", but true for "1.13.0" and "1.12.2".
     * @param versionStr Version to test if greater
     * @param otherStr Other version
     * @return Whether <code>versionStr</code> is greater than or equal.
     */
    public static boolean isVersionGreaterOrEq(String versionStr, String otherStr) {
        String[] version = splitVersionString(versionStr);
        String[] other = splitVersionString(otherStr);
        // If the other version has more numbers, it must be newer
        if(other.length > version.length) {return false;}

        for(int i = 0; i < version.length; i++) {
            int number = Integer.parseInt(version[i]);
            if(i >= other.length) {
                return true;
            }

            // If one of then numbers in the other version is greater than this one, return false
            int otherNumber = Integer.parseInt(other[i]);
            if(otherNumber > number) {return false;}

            // If one of the numbers in this version is greater than the other one, return true
            if(number > otherNumber) {return true;}
        }

        // If all numbers are equal, return true
        return true;
    }

    private static String[] splitVersionString(String version) {
        String[] result = version.split("\\.");
        List<String> finalResult = new ArrayList<>();

        // Also split by dashes
        for(String str : result) {
            finalResult.addAll(Arrays.asList(str.split("-")));
        }

        // Remove "R" characters from the version
        for(int i = 0; i < finalResult.size(); i++) {
            finalResult.set(i, finalResult.get(i).replace("R", ""));
        }

        return finalResult.toArray(new String[0]);
    }

    /**
     * Finds if the current minecraft server version is greater than <code>version</code>.
     * @param version The version to test
     * @return Whether the current minecraft server version is greater than <code>version</code>.
     */
    public static boolean isMcVersionAtLeast(String version) {
        Boolean existing = versionAtLeastCache.get(version);
        if(existing != null) {return existing;}

        boolean result = isVersionGreaterOrEq(getCurrentVersion(), version);
        versionAtLeastCache.put(version, result);
        return result;
    }
}
