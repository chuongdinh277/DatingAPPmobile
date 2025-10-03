package com.example.btl_mobileapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class NameCache {
    private static final String SP = "profile_cache";
    private static final String KEY_CURRENT_NAME = "name_current";
    private static final String KEY_PARTNER_ID = "partner_id";
    private static final String KEY_PARTNER_NAME = "name_partner";

    private static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    public static String getCurrentName(Context ctx) {
        return sp(ctx).getString(KEY_CURRENT_NAME, null);
    }

    public static void setCurrentName(Context ctx, String name) {
        sp(ctx).edit().putString(KEY_CURRENT_NAME, name).apply();
    }

    public static String getPartnerId(Context ctx) {
        return sp(ctx).getString(KEY_PARTNER_ID, null);
    }

    public static void setPartnerId(Context ctx, String partnerId) {
        sp(ctx).edit().putString(KEY_PARTNER_ID, partnerId).apply();
    }

    public static String getPartnerName(Context ctx) {
        return sp(ctx).getString(KEY_PARTNER_NAME, null);
    }

    public static void setPartnerName(Context ctx, String partnerName) {
        sp(ctx).edit().putString(KEY_PARTNER_NAME, partnerName).apply();
    }

    public static void setPartner(Context ctx, String partnerId, String partnerName) {
        SharedPreferences.Editor e = sp(ctx).edit();
        e.putString(KEY_PARTNER_ID, partnerId);
        e.putString(KEY_PARTNER_NAME, partnerName);
        e.apply();
    }

    public static void clearPartner(Context ctx) {
        SharedPreferences.Editor e = sp(ctx).edit();
        e.remove(KEY_PARTNER_ID);
        e.remove(KEY_PARTNER_NAME);
        e.apply();
    }

    public static void clearAll(Context ctx) {
        sp(ctx).edit().clear().apply();
    }
}