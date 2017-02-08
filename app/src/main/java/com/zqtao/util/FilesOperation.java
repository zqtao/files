package com.zqtao.util;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.zqtao.files.FilesActivity;
import com.zqtao.files.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by zqtao on 15-11-3.
 */
public class FilesOperation extends AsyncTask<Integer, Integer, Boolean> {
    private List<String> selectedFiles;
    private int operationType;
    private String destPath;

    private ProgressDialog progressDialog;
    private FilesActivity filesActivity;

    public FilesOperation(FilesActivity filesActivity, List<String> selectedFiles, int operationType) {
        this.filesActivity = filesActivity;
        this.selectedFiles = selectedFiles;
        this.operationType = operationType;
    }

    public void setCopyPath(String destPath) {
        this.destPath = destPath;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(filesActivity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle(R.string.action_delete);
        progressDialog.setIcon(R.drawable.delete);
        progressDialog.setMax(selectedFiles.size());
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancel(true);
            }
        });
        progressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Integer... params) {
        for (int currentIndex = 0; currentIndex < selectedFiles.size(); ++currentIndex) {
            switch (operationType) {
                case FilesActivity.VIEW_STATUS_DELETE:
                    deleteFile(currentIndex);
                    break;
                case FilesActivity.VIEW_STATUS_MOVE:
                    moveFile(currentIndex);
                    break;
                case FilesActivity.VIEW_STATUS_COPY:
                    copyFile(currentIndex);
                    break;
                default:
                    break;
            }
            publishProgress(currentIndex);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.i(FilesActivity.logTag, e.toString());
            }
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        progressDialog.setProgress(values[0]);
    }

    @Override
    protected void onCancelled() {
        progressDialog.cancel();
        filesActivity.backwardFromEdit(true);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        progressDialog.setProgress(selectedFiles.size());
        progressDialog.cancel();
        String strResult = null;
        if (result) {
            switch (operationType) {
                case FilesActivity.VIEW_STATUS_DELETE:
                    strResult = "Delete completed!";
                    break;
                case FilesActivity.VIEW_STATUS_MOVE:
                    strResult = "Move completed!";
                    break;
                case FilesActivity.VIEW_STATUS_COPY:
                    strResult = "Copy completed!";
                    break;
                default:
                    break;
            }
        } else {
            switch (operationType) {
                case FilesActivity.VIEW_STATUS_DELETE:
                    strResult = "Delete failed!";
                    break;
                case FilesActivity.VIEW_STATUS_MOVE:
                    strResult = "Move failed!";
                    break;
                case FilesActivity.VIEW_STATUS_COPY:
                    strResult = "Copy failed!";
                    break;
                default:
                    break;
            }
        }
        Toast.makeText(filesActivity, strResult, Toast.LENGTH_SHORT).show();
        filesActivity.backwardFromEdit(true);
    }

    public void deleteFile(int index) {
        deleteFile(selectedFiles.get(index));
    }

    public void deleteFile(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File newFile : files) {
                deleteFile(newFile.getAbsolutePath());
            }
        }
        if (file.delete()) {
            Log.i(FilesActivity.logTag, "Delete directory success!");
        }
    }

    public void moveFile(int index) {
        moveFile(selectedFiles.get(index), destPath);
    }

    public void moveFile(String path, String dest) {
        File file = new File(path);
        String fileName = file.getName();
        String strNewDir = dest + "/" + fileName;
        if (file.isDirectory()) {
            File newDir = new File(strNewDir);
            if (!newDir.exists()) {
                if (newDir.mkdir()) {
                    Log.i(FilesActivity.logTag, "Create directory success!");
                }
            }
            File[] files = file.listFiles();
            for (File newFile : files) {
                moveFile(newFile.getAbsolutePath(), strNewDir);
            }
        }
        if (file.renameTo(new File(strNewDir))) {
            Log.i(FilesActivity.logTag, "Rename directory success!");
        }
    }

    public void copyFile(int index) {
        String path = selectedFiles.get(index);
        File file = new File(path);
        String fileName = file.getName();
        String strNewDir = destPath + "/" + fileName;
        if (file.isDirectory()) {
            copyFolder(path, strNewDir);
        } else {
            copyFile(path, strNewDir);
        }
    }

    public void copyFolder(String fromPath, String toPath) {
        File newDir = new File(toPath);
        if (!newDir.exists()) {
            if(newDir.mkdir()) {
                Log.i(FilesActivity.logTag, "Create directory success!");
            }
        }
        File file = new File(fromPath);
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        for (File newFile : files) {
            if (newFile.isFile()) {
                copyFile(newFile.getAbsolutePath(), toPath+"/"+newFile.getName());
            } else {
                copyFolder(newFile.getAbsolutePath(), toPath+"/"+newFile.getName());
            }
        }
    }

    public void copyFile(String fromPath, String toPath) {
        File fromFile = new File(fromPath);
        File toFile = new File(toPath);

        if (!fromFile.exists()) {
            return;
        }
        if (!fromFile.isFile()) {
            return;
        }
        if (!fromFile.canRead()) {
            return;
        }
        if (toFile.exists()) {
            String parentDir = toFile.getParent();
            String fileName = toFile.getName();
            String onlyFileName = fileName.substring(0, fileName.lastIndexOf('.'));
            String ext = null;
            if (fileName.lastIndexOf('.') != -1) {
                ext = fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length());
            }
            int index = 1;
            do {
                toPath = parentDir + "/" + onlyFileName + "_" + String.valueOf(index);
                if (ext != null) {
                    toPath = toPath + "." + ext;
                }
                toFile = new File(toPath);
                ++index;
            } while (toFile.exists());
        }
        try {
            FileInputStream fosfrom = new FileInputStream(fromFile);
            FileOutputStream fosto = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = fosfrom.read(bt)) > 0) {
                fosto.write(bt, 0, c);
            }
            fosfrom.close();
            fosto.close();
        } catch (Exception e) {
            Log.e(FilesActivity.logTag, e.getMessage());
        }
    }
}
