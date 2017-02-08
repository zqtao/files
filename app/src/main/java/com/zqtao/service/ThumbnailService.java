package com.zqtao.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Handler;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;

import com.zqtao.db.ThumbnailData;
import com.zqtao.files.FilesActivity;
import com.zqtao.files.MainActivity;
import com.zqtao.util.MediaFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
* Generate thumbnail, size: MICRO_KIND 96 x 96
* */

public class ThumbnailService extends Service {
    private ServiceHandler serviceHandler = null;
    private static Handler filesHandler = null;
    private static List<String> pathList = null;

    static private final int width = 96;
    static private final int height = 96;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            synchronized(this) {
                String path = (String)msg.obj;
                int fileType = msg.what;
                Log.i(FilesActivity.logTag, "path:"+path);

                Bitmap bitmap = null;
                if (fileType >= MediaFile.FILE_TYPE_MP4 && fileType <= MediaFile.FILE_TYPE_WMV) {
                    bitmap = getVideoThumbnail(path);
                } else if (fileType >= MediaFile.FILE_TYPE_JPEG && fileType <= MediaFile.FILE_TYPE_WBMP) {
                    bitmap = getImageThumbnail(path);
                }
                if (bitmap != null) {
                    String thumbnailPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/.zqtao_thumbnail";
                    File thumbnailDir = new File(thumbnailPath);
                    if (!thumbnailDir.exists()) {
                        if (thumbnailDir.mkdir()) {
                            Log.i(FilesActivity.logTag, "Create directory success!");
                        }
                    }
                    Date now = new Date(System.currentTimeMillis());
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.CHINA);
                    thumbnailPath = thumbnailPath+"/."+dateFormat.format(now)+".jpg";

                    saveBitmapFile(bitmap, thumbnailPath);
                    /* 发消息到FilesActivity， 更新View，保存数据到数据库 */
                    ThumbnailData thumbnailData = new ThumbnailData();
                    thumbnailData.setFilePath(path);
                    thumbnailData.setThumbnailPath(thumbnailPath);

                    if (filesHandler != null) {
                        Message sendMsg = filesHandler.obtainMessage();
                        sendMsg.what = 0;
                        sendMsg.obj = thumbnailData;
                        filesHandler.sendMessage(sendMsg);
                        pathList.remove(path);
                    } else {
                        Log.i(FilesActivity.logTag, "filesHandler is null");
                    }
                } else {
                    Log.i(FilesActivity.logTag, "bitmap is null");
                }

                stopSelf(msg.arg1);
            }
        }
    }

    public ThumbnailService() {
    }

    public static void setMainHandler(Handler mainHandler) {
        if (filesHandler == null) {
            filesHandler = mainHandler;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("ThumbnailService", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        if (serviceHandler == null) {
            serviceHandler = new ServiceHandler(thread.getLooper());
        }
        if (pathList == null) {
            pathList = new ArrayList<>();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        String path = (String)bundle.get(MainActivity.imagePath);
        if (path != null && !path.isEmpty()) {
            if (pathList.indexOf(path) == -1) {
                Message msg = serviceHandler.obtainMessage();
                msg.obj = path;
                msg.what = MediaFile.getDetailFileType(path);
                msg.arg1 = startId;
                serviceHandler.sendMessage(msg);
            }
        } else {
            Log.e(FilesActivity.logTag, "File path is empty!");
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pathList.clear();
    }

    /**
     * 根据指定的图像路径和大小来获取缩略图
     * 此方法有两点好处：
     *     1. 使用较小的内存空间，第一次获取的bitmap实际上为null，只是为了读取宽度和高度，
     *        第二次读取的bitmap是根据比例压缩过的图像，第三次读取的bitmap是所要的缩略图。
     *     2. 缩略图对于原图像来讲没有拉伸，这里使用了2.2版本的新工具ThumbnailUtils，使
     *        用这个工具生成的图像不会被拉伸。
     * @param imagePath 图像的路径
     * @return 生成的缩略图
     */
    private Bitmap getImageThumbnail(String imagePath) {
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // 获取这个图片的宽和高，注意此处的bitmap为null
        BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false; // 设为 false
        // 计算缩放比
        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / width;
        int beHeight = h / height;
        int be;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        // 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        // 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    /**
     * 获取视频的缩略图
     * 先通过ThumbnailUtils来创建一个视频的缩略图，然后再利用ThumbnailUtils来生成指定大小的缩略图。
     * 如果想要的缩略图的宽和高都小于MICRO_KIND，则类型要使用MICRO_KIND作为kind的值，这样会节省内存。
     * @param videoPath 视频的路径
     * @return 指定大小的视频缩略图
     */
    private Bitmap getVideoThumbnail(String videoPath) {
        /*参照MediaStore.Images.Thumbnails类中的常量MINI_KIND和MICRO_KIND。
         *其中，MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96*/
        return ThumbnailUtils.createVideoThumbnail(videoPath,
                MediaStore.Video.Thumbnails.MICRO_KIND);
    }

    public void saveBitmapFile(Bitmap bitmap, String thumbnailPath){
        File file = new File(thumbnailPath);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkPathExist(String path) {
        if (pathList == null) {
            return true;
        }
        if (pathList.indexOf(path) == -1) {
            return true;
        }
        return false;
    }
}
