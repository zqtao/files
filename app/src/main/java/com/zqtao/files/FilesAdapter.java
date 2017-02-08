package com.zqtao.files;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zqtao.db.FilesData;
import com.zqtao.service.ThumbnailService;
import com.zqtao.util.MediaFile;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zqtao on 15-10-23.
 */
public class FilesAdapter extends BaseAdapter {
    private List<Map<String, Object>> fileData;
    private List<HolderView> holderViewList;
    private FilesActivity context;
    private boolean isEdit;
    FilesData filesData;

    FilesAdapter(FilesActivity context, List<Map<String, Object>> fileData) {
        super();
        this.context = context;
        this.fileData = fileData;
        isEdit = false;
        filesData = context.getFilesData();
        holderViewList = new ArrayList<>();
    }

    @Override
    public int getCount() {
        Log.i(FilesActivity.logTag, "Count:"+fileData.size());
        return fileData.size();
    }

    @Override
    public Object getItem(int postion) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        HolderView holderView;
        if (view == null) {
            holderView = new HolderView();
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.file_item, null);
            holderView.fileType = (ImageView)view.findViewById(R.id.file_type);
            holderView.fileName = (TextView)view.findViewById(R.id.file_name);
            holderView.fileTime = (TextView)view.findViewById(R.id.file_time);
            holderView.selected = (CheckBox)view.findViewById(R.id.file_selected);
            view.setTag(holderView);
        } else {
            holderView = (HolderView)view.getTag();
        }
        if (holderViewList.indexOf(holderView) == -1) {
            holderViewList.add(holderView);
        }
        holderView.position = position;
        Map<String, Object> map = fileData.get(position);
        boolean isFolder = (boolean)map.get(FilesActivity.strFileIsFolder);
        if (isFolder) {
            holderView.fileType.setImageResource(R.drawable.folder);
        } else {
            /* 设置thumbnail，如果不存在thumbnail启动service生成 */
            String filePath = (String)map.get(FilesActivity.strFilePath);
            if (MediaFile.isImageFileType(filePath) || MediaFile.isVideoFileType(filePath)) {
                String thumbnailPath = (String)map.get(FilesActivity.strFileThumbnail);
                if (thumbnailPath == null || thumbnailPath.isEmpty()) {
                    holderView.fileType.setImageResource(R.drawable.file);
                } else {
                    Bitmap bitmap = BitmapFactory.decodeFile(thumbnailPath); ;
                    holderView.fileType.setImageBitmap(bitmap);
                }
            } else if (MediaFile.isAudioFileType(filePath)) {
                holderView.fileType.setImageResource(R.drawable.music);
            } else {
                holderView.fileType.setImageResource(R.drawable.file);
            }
        }
        holderView.fileName.setText((String)map.get(FilesActivity.strFileName));
        holderView.fileTime.setText((String) map.get(FilesActivity.strFileTime));

        holderView.selected.setTag(position);
        holderView.selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int position = (int) buttonView.getTag();
                HashMap<String, Object> map = (HashMap<String, Object>) fileData.get(position);
                map.put(FilesActivity.strFileSelected, isChecked);

                context.putSelectedFile((String)map.get(FilesActivity.strFilePath), isChecked);
                context.setTitleString(true);
            }
        });

        if (isEdit) {
            holderView.selected.setVisibility(View.VISIBLE);
            holderView.selected.setChecked((boolean) map.get(FilesActivity.strFileSelected));
        } else {
            holderView.selected.setVisibility(View.GONE);
        }
        return view;
    }

    public void setEditMode(boolean isEdit) {
        this.isEdit = isEdit;
    }

    public boolean getEditMode() {
        return isEdit;
    }

    public void updateFileType(String filePath, String thumbnailPath) {
        int position = -1;
        for (int i = 0; i < fileData.size(); ++i) {
            HashMap<String, Object> map = (HashMap<String, Object>)fileData.get(i);
            String path = (String)map.get(FilesActivity.strFilePath);
            if (path.compareTo(filePath) == 0) {
                position = i;
                map.put(FilesActivity.strFileThumbnail, thumbnailPath);
                break;
            }
        }
        for (int i = 0; i < holderViewList.size(); ++i) {
            HolderView holderView = holderViewList.get(i);
            if (holderView.position == position) {
                Bitmap bitmap = BitmapFactory.decodeFile(thumbnailPath);
                holderView.fileType.setImageBitmap(bitmap);
                break;
            }
        }
    }

    class HolderView {
        ImageView fileType;
        TextView fileName;
        TextView fileTime;
        CheckBox selected;
        int position;
    }
}
