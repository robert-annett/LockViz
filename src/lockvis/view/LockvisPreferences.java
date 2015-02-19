package lockvis.view;

import java.util.prefs.Preferences;

public class LockvisPreferences {
    
    enum PreferenceKey {
        MAIN_HEIGHT("1024"),
        MAIN_WIDTH("1280"),
        STATS_DIVIDER_LOCATION("200"),
        HORIZONTAL_DIVIDER_LOCATION("500"),
        VERTICAL_DIVIDER_LOCATION("250"),
        CTLPNL_DIVIDER_LOCATION("250");

        private final String defaultValue;
        private PreferenceKey(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    };

    private static Preferences preferences = Preferences.userRoot().node("/lockviz");
    
    
    public static int getInt(PreferenceKey key)    {
        return preferences.getInt(key.toString(), Integer.valueOf(key.defaultValue));
    }

    public static void putInt(PreferenceKey key, int value)    {
        preferences.putInt(key.toString(), Integer.valueOf(value));
    }
    
    
}
