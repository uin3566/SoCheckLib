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
    private boolean isGetFileMd5Exception = false;
    private Context context;
    private SharedPrefUtil sharedPref;

    private String configKey;
    public static final String SO_MD5_CHECKED = "so_md5_checked";

    private SoChecker() {
        configKey = SO_MD5_CHECKED;
    }

    private static class Holder {
        private static SoChecker instance = new SoChecker();
    }

    public static SoChecker getInstance() {
        return Holder.instance;
    }

    public void init(Context context, String sharedPreferencesConfigFileName) {
        this.context = context.getApplicationContext();
        sharedPref = new SharedPrefUtil(context, sharedPreferencesConfigFileName);
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public boolean checkMd5(String currentVersion, String md5FileName) {
        if (isMd5Checked(currentVersion)) {
            Log.i(TAG, "md5 has checked before");
            return true;
        }

        //计算所有so的md5并将计算结果写入配置
        return writeConfig(currentVersion, md5FileName);
    }

    private boolean isMd5Checked(String currentVersion) {
        String appVersion = sharedPref.getConfig(configKey);
        return appVersion.equals(currentVersion);
    }

    private boolean writeConfig(String currentVersion, String md5FileName) {
        long startTime = System.currentTimeMillis();
        Map<String, String> calculatedMd5s = getCalculatedMd5s(md5FileName);
        Log.i(TAG, "read calculated md5 consume " + (System.currentTimeMillis() - startTime) + "ms");
        Map<String, String> currentMd5s = getCurrentMd5s();
        Log.i(TAG, "calculate apk so md5 consume " + (System.currentTimeMillis() - startTime) + "ms");

        if (calculatedMd5s == null || currentMd5s == null || (isGetFileMd5Exception && currentMd5s.size() != calculatedMd5s.size())) {
            Log.i(TAG, "check io exception, can not finish check process, do not write config, and continue process, check at next startup");
            return true;
        }

        if (currentMd5s.size() != calculatedMd5s.size()) {
            Log.i(TAG, "the numbers of so in setup apk is not correct, setup not fully");
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
            sharedPref.writeConfig(configKey, currentVersion);
        }

        Log.i(TAG, "compare md5 consume " + (System.currentTimeMillis() - startTime) + "ms");

        return result;
    }

    //获得构建时的md5
    private Map<String, String> getCalculatedMd5s(String md5FileName) {
        StringBuilder buffer = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
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

        String json = buffer.toString();
        return (Map<String, String>) JSON.parse(json);
    }

    //获得安装后的md5
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
