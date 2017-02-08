package com.zqtao.files;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.format.Formatter;

import java.util.List;
import java.util.Map;

/**
 * Created by zqtao on 2015/11/11.
 */
public class MainAdapter extends BaseAdapter {
    private MainActivity mainActivity = null;
    List<Map<String, Object>> sdcardData = null;

    MainAdapter(MainActivity mainActivity, List<Map<String, Object>> sdcardData) {
        super();
        this.mainActivity = mainActivity;
        this.sdcardData = sdcardData;
    }

    @Override
    public int getCount() {
        return sdcardData.size();
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
            LayoutInflater inflater = LayoutInflater.from(mainActivity);
            view = inflater.inflate(R.layout.sdcard_list, viewGroup, false);
            holderView.storageType = (ImageView)view.findViewById(R.id.storage_type);
            holderView.storageName = (TextView)view.findViewById(R.id.sdcard_name);
            holderView.storageStatue = (TextView)view.findViewById(R.id.sdcard_stroage);
            holderView.progressBar = (ProgressBar)view.findViewById(R.id.sdcard_status);
            view.setTag(holderView);
        } else {
            holderView = (HolderView)view.getTag();
        }
        Map<String, Object> map = sdcardData.get(position);
        int storageType = (int)map.get(MainActivity.storageType);
        if (storageType == MainActivity.storageTypeSdcard) {
            holderView.storageType.setBackgroundResource(R.drawable.sdcard);
        } else {
            holderView.storageType.setBackgroundResource(R.drawable.cloud);
        }
        holderView.storageName.setText((String) map.get(MainActivity.storageName));
        long totalStorage = (long)map.get(MainActivity.storageTotal);
        long usedStorage = (long)map.get(MainActivity.storageUsed);
        String strTotalStorage = Formatter.formatFileSize(mainActivity, totalStorage);
        String strUsedStorage = Formatter.formatFileSize(mainActivity, usedStorage);
        String strStorageStatus = strUsedStorage+"/"+strTotalStorage;
        holderView.storageStatue.setText(strStorageStatus);
        holderView.progressBar.setProgress((int)(usedStorage*100/totalStorage));

        return view;
    }

    class HolderView {
        ImageView storageType;
        TextView storageName;
        TextView storageStatue;
        ProgressBar progressBar;
    }
}
