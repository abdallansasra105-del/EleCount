package com.example.elecount.Hellper;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.elecount.models.User;

/**
 * حفظ واستعادة جلسة المستخدم محلياً
 */
public final class UserSession {

    public static final String PREFS_NAME = "EleCountPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_GUEST = "isGuest";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_IMAGE = "userImageUrl";

    private UserSession() {
    }

    public static boolean isLoggedIn(Context context) {
        return prefs(context).getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static boolean isGuest(Context context) {
        return prefs(context).getBoolean(KEY_IS_GUEST, false);
    }

    /** وضع الضيف: المحاكي فقط */
    public static void enterGuestMode(Context context) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putBoolean(KEY_IS_GUEST, true);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_IMAGE);
        editor.apply();
    }

    public static void save(Context context, User user) {
        if (user == null) {
            return;
        }
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putBoolean(KEY_IS_GUEST, false);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USER_NAME, user.getName());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        if (user.getImageUrl() != null) {
            editor.putString(KEY_USER_IMAGE, user.getImageUrl());
        } else {
            editor.remove(KEY_USER_IMAGE);
        }
        editor.apply();
    }

    public static User load(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return null;
        }
        User user = new User();
        user.setId(prefs.getString(KEY_USER_ID, ""));
        user.setName(prefs.getString(KEY_USER_NAME, ""));
        user.setEmail(prefs.getString(KEY_USER_EMAIL, ""));
        user.setImageUrl(prefs.getString(KEY_USER_IMAGE, null));
        return user;
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    public static void applyToDataManager(DataManager dataManager, Context context) {
        User user = load(context);
        if (user != null && user.getId() != null && !user.getId().isEmpty()) {
            dataManager.setCurrentUser(user);
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
