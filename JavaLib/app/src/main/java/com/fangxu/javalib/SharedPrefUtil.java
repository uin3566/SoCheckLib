package com.fangxu.javalib;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Administrator on 2016/11/28.
 */
public class SharedPrefUtil {
    private SharedPreferences sp;

    public SharedPrefUtil(Context context, String configFileName) {
        sp = context.getSharedPreferences(configFileName, Context.MODE_PRIVATE);
    }

    public void writeConfig(String key, String value) {
        sp.edit().putString(key, value).commit();
    }

    public String getConfig(String key) {
        return sp.getString(key, "");
    }
}
