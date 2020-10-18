package com.gomihagle.photogallery.model;

import android.content.Context;
import android.preference.PreferenceManager;

public class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "search_query";

    public static String getStoredQuery(Context context)
    {
       return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext())
                .getString(PREF_SEARCH_QUERY,null);

    }

    public static void setStoredQuery(Context context,String query)
    {
        PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext())
                .edit()
                .putString(PREF_SEARCH_QUERY,query)
                .apply();
    }
}
