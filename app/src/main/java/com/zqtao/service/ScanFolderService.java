package com.zqtao.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.webkit.MimeTypeMap;

import java.io.File;

public class ScanFolderService extends Service {
    private static final String audioType = "audio/*";
    private static final String imageType = "image/*";
    private static final String videoType = "video/*";
    private static final String textType = "text/*";
    private static final String wordType = "*word";
    private static final String excelType = "*excel";
    private static final String pptType = "*powerpoint";

    public ScanFolderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void scanFiles(String path) {
        File scanFile = new File(path);
        if (scanFile.exists()) {
            if (scanFile.isFile()) {
                /* File type: 1 -- 声音  2 -- 图片  3 -- 视频  4 -- 文档 */
                int type = 0;
                String strMemi = getMemiType(path);
                if (strMemi.matches(audioType)) {
                    type = 1;
                } else if (strMemi.matches(imageType)) {
                    type = 2;
                } else if (strMemi.matches(videoType)) {
                    type = 3;
                } else if (strMemi.matches(textType) ||
                        strMemi.matches(wordType) ||
                        strMemi.matches(excelType) ||
                        strMemi.matches(pptType)) {
                    type = 3;
                }
            } else {
                File[] files = scanFile.listFiles();
                for (File file : files) {
                    scanFiles(file.getAbsolutePath());
                }
            }
        }
    }

    private String getMemiType(String path) {
        File file = new File(path);
        String suffix = "";
        String name = file.getName();
        final int idx = name.lastIndexOf(".");
        if (idx > 0) {
            suffix = name.substring(idx + 1);
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
    }
}
