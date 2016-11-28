package com.fangxu.javalib;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Administrator on 2016/11/28.
 */
public class Md5Util {
    private static final String TAG = "Md5Util";
    private static MessageDigest messageDigest;

    public static String getFileMD5String(File file) {
        if (messageDigest == null) {
            synchronized (Md5Util.class) {
                if (messageDigest == null) {
                    try {
                        messageDigest = MessageDigest.getInstance("MD5");
                    } catch (NoSuchAlgorithmException e) {
                        Log.i(TAG, e.toString());
                        return null;
                    }
                }
            }
        }

        byte[] buffer = new byte[1024];
        int numRead;
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);
            while ((numRead = fis.read(buffer)) > 0) {
                messageDigest.update(buffer, 0, numRead);
            }
        } catch (IOException e) {
            Log.i(TAG, e.toString());
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return bufferToHex(messageDigest.digest());
    }

    private static String bufferToHex(byte[] bytes) {
        String returnVal = "";
        for (int i = 0; i < bytes.length; i++) {
            returnVal += Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toLowerCase();
    }
}
