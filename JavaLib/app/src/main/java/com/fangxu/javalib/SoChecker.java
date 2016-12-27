package com.fangxu.javalib;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SoChecker {
    private static final String TAG = "SoChecker";
    private static final String PREFERENCE_NAME = "shared_pref_config";
    private static final String SO_MD5_CHECKED_VERSION = "so_md5_checked_version";
    private static final String SO_FILE_DAMAGED = "so_file_damaged";

    private boolean isGetFileMd5Exception = false;
    private Context context;
    private SharedPrefUtil sharedPref;

    private SoChecker() {
    }

    private static class Holder {
        private static SoChecker instance = new SoChecker();
    }

    public static SoChecker getInstance() {
        return Holder.instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        sharedPref = new SharedPrefUtil(context, PREFERENCE_NAME);
    }

    public boolean checkMd5(String currentVersion) {
        if (isMd5Checked(currentVersion)) {
            Log.i(TAG, "md5 has checked before");
            return true;
        }

        //get all so md5s and write the config
        return writeConfig(currentVersion);
    }

    private boolean isMd5Checked(String currentVersion) {
        String oldVersion = sharedPref.getStringConfig(SO_MD5_CHECKED_VERSION);
        return oldVersion.equals(currentVersion);
    }

    private boolean writeConfig(String currentVersion) {
        long startTime = System.currentTimeMillis();
        Map<String, String> calculatedMd5s = getCalculatedMd5s();
        Log.i(TAG, "read calculated md5 consume " + (System.currentTimeMillis() - startTime) + "ms");
        Map<String, String> currentMd5s = getCurrentMd5s();
        Log.i(TAG, "calculate apk so md5 consume " + (System.currentTimeMillis() - startTime) + "ms");

        if (calculatedMd5s == null || currentMd5s == null || (isGetFileMd5Exception && currentMd5s.size() != calculatedMd5s.size())) {
            Log.i(TAG, "check io exception, can not finish check process, do not write config, and continue process, check at next startup");
            return true;
        }

        if (currentMd5s.size() != calculatedMd5s.size()) {
            Log.i(TAG, "the numbers of so in setup apk is not correct, setup not fully");
            sharedPref.setBooleanConfig(SO_FILE_DAMAGED, true);
            return false;
        }

        boolean result = true;
        Iterator<Map.Entry<String, String>> iterator = calculatedMd5s.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();

            if (currentMd5s.containsKey(entry.getKey())) {
                if (!currentMd5s.get(entry.getKey()).equals(entry.getValue())) {
                    String log = String.format("check md5 different, break!!%s, calculatedMd5=%s, currentMd5=%s",
                            entry.getKey(), entry.getValue(), currentMd5s.get(entry.getKey()));
                    Log.i(TAG, log);
                    result = false;
                    break;
                }
            }
        }

        if (result) {
            sharedPref.setStringConfig(SO_MD5_CHECKED_VERSION, currentVersion);
            sharedPref.setBooleanConfig(SO_FILE_DAMAGED, false);
        } else {
            sharedPref.setBooleanConfig(SO_FILE_DAMAGED, true);
        }

        Log.i(TAG, "compare md5 consume " + (System.currentTimeMillis() - startTime) + "ms");

        return result;
    }

    //get so md5s in md5 file that generated during apk build
    private Map<String, String> getCalculatedMd5s() {
        StringBuilder buffer = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            String md5FileName = BuildConfig.FLAVOR + BuildConfig.BUILD_TYPE + "md5.txt";
            Log.i(TAG, "md5 file name " + md5FileName);
            inputStream = context.getAssets().open(md5FileName);
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            buffer = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            Log.i(TAG, e.toString());
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            String json = buffer.toString();
            return (Map<String, String>) JSON.parse(json);
        } catch (Exception e) {

        }

        return null;
    }

    //get so md5s during runtime after installed
    private Map<String, String> getCurrentMd5s() {
        String libraryPath = context.getApplicationInfo().dataDir + File.separator + "lib";
        File file = new File(libraryPath);
        if (!file.isDirectory()) {
            return null;
        }

        Map<String, String> md5s = new HashMap<>();
        File[] files = file.listFiles();
        for (File f : files) {
            Log.i(TAG, f.getName());
            if (!isSoFile(f)) {
                continue;
            }
            String md5 = Md5Util.getFileMD5String(f);
            if (md5 != null) {
                md5s.put(f.getName(), md5);
            } else {
                isGetFileMd5Exception = true;
            }
        }

        return md5s;
    }

    private boolean isSoFile(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= fileName.length() - 1) {
            return false;
        }

        String ext = fileName.substring(dotIndex + 1);
        return "so".equals(ext);
    }
}
