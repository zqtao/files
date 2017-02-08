package com.zqtao.db;

import android.os.Handler;

/**
 * Created by zqtao on 15-11-16.
 */
public class ThumbnailData {
    private String filePath;
    private String thumbnailPath;

    public String getFilePath() {
        return filePath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
}
