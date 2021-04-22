package org.studip.unofficial_app.model;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.security.crypto.EncryptedSharedPreferences;

public class Settings
{
    public static final int AUTHENTICATION_BASIC = 1;
    public static final int AUTHENTICATION_COOKIE = 2;
    public static final int AUTHENTICATION_OAUTH = 3;
    
    public static final String LOGOUT_KEY = "logout";
    public volatile boolean logout;
    
    public static final int authentication_method_default = AUTHENTICATION_COOKIE;
    private static final String authentication_method_key = "authentification_method";
    public volatile int authentication_method;
    
    private static final String theme_key = "theme";
    public volatile int theme;
    
    
    private static final String notification_period_key = "notification_period";
    public volatile int notification_period;
    
    
    private static final String notification_service_enabled_key = "notification_service_enabled";
    public volatile boolean notification_service_enabled;
    
    public Settings()
    {
        defaults();
    }
    private void defaults() {
        logout = false;
        authentication_method = authentication_method_default;
        theme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        notification_period = 30;
        notification_service_enabled = false;
    }
    public static Settings load(SharedPreferences prefs)
    {
        Settings s = new Settings();
        
        if (prefs instanceof EncryptedSharedPreferences)
        {
            s.logout = prefs.getBoolean(LOGOUT_KEY,false);
            s.authentication_method = prefs.getInt(authentication_method_key,authentication_method_default);
            s.theme = prefs.getInt(theme_key,AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            s.notification_period = prefs.getInt(notification_period_key,30);
            s.notification_service_enabled = prefs.getBoolean(notification_service_enabled_key,false);
        }
        else
        {
            s.defaults();
        }
        return s;
    }
    
    
    @SuppressLint("ApplySharedPref")
    public void safe(SharedPreferences prefs)
    {
        if (prefs instanceof  EncryptedSharedPreferences)
        {
            SharedPreferences.Editor e = prefs.edit();
            e.putBoolean(LOGOUT_KEY,logout);
            e.putInt(authentication_method_key,authentication_method);
            e.putInt(theme_key,theme);
            e.putInt(notification_period_key,notification_period);
            e.putBoolean(notification_service_enabled_key,notification_service_enabled);
            
            if (logout) {
                e.commit();
            } else
            {
                e.apply();
            }
        }
        else
        {
            System.out.println("not saving settings as shared preferences aren't encrypted");
        }
    }
    
    
}
