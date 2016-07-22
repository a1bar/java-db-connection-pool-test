package pl.edu.pja.master.s12621.utils;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by bartosz.drabik
 */
public class AppProfiles {
    private final static Set PROFILES = new LinkedHashSet(Arrays.asList("dbcp1", "dbcp2", "bonecp", "hikari"));
    private final static String DEFAULT_PROFILE = (String) PROFILES.iterator().next();

    public static String getAppProfile(String profileName) {
        if (Strings.isNullOrEmpty(profileName)) {
            return DEFAULT_PROFILE;
        }
        return PROFILES.contains(profileName) ? profileName : DEFAULT_PROFILE;
    }

    public static String getDefaultProfile() {
        return DEFAULT_PROFILE;
    }
}
