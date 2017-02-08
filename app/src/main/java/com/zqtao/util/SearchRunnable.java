package com.zqtao.util;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.zqtao.files.FilesActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by zqtao on 15-11-19.
 */
public class SearchRunnable implements Runnable {
    private String searchKey;
    private String currentPath;
    private static Handler handler;
    private boolean isCheckHiddenFile = false;

    public SearchRunnable() {
    }

    @Override
    public void run() {
        initSearchData(searchKey, currentPath);
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setCheckHiddenFile(boolean isCheckHiddenFile) {
        this.isCheckHiddenFile = isCheckHiddenFile;
    }

    private void initSearchData(String searchKey, String path) {
        File file = new File(path);
        if (!isCheckHiddenFile) {
            char startC = file.getName().charAt(0);
            if (startC == '.') {
                return;
            }
        }

        Log.i(FilesActivity.logTag, "Search Path:" + path);

        String name = file.getName();
        if (name.contains(searchKey)) {
            Log.i(FilesActivity.logTag, "Search Path:"+path+"inlcude to search result");
            Map<String, Object> map = new HashMap<>();
            map.put(FilesActivity.strFileName, file.getName());
            map.put(FilesActivity.strFilePath, file.getAbsolutePath());
            map.put(FilesActivity.strFileIsFolder, file.isDirectory());
            map.put(FilesActivity.strFileSelected, false);
            /* Set file's latest modify time */
            SimpleDateFormat sdf = new SimpleDateFormat(FilesActivity.strFileTimeFormat, Locale.CHINA);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(file.lastModified());
            map.put(FilesActivity.strFileTime, sdf.format(cal.getTime()));

            /* 发送更新file list view请求 */
            Message msg = handler.obtainMessage();
            msg.what = 1;
            msg.obj = map;
            handler.sendMessage(msg);
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File newFile: files) {
                initSearchData(searchKey, newFile.getAbsolutePath());
            }
        }
    }
}
