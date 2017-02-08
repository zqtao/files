package com.zqtao.files;

import android.content.Intent;
import android.os.Environment;
import android.os.StatFs;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String storageType = "storageType";
    public static final String storageName = "storageName";
    public static final String storagePath = "storagePath";
    public static final String storageTotal = "totalStorage";
    public static final String storageUsed = "sdcardUsedStorage";
    public static final String imagePath = "imagePath";

    public static final int storageTypeSdcard = 1;
//    public static final int storageTypeCloud = 2;

    private static final String sdcardPath = "/storage/sdcard1/";

    private List<Map<String, Object>> storageTypeData = null;

    private ListView sdcardListView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (sdcardListView == null) {
            sdcardListView = (ListView)findViewById(R.id.main_list);
        }
        initData();
        MainAdapter adapter = new MainAdapter(this, storageTypeData);
        sdcardListView.setAdapter(adapter);

        sdcardListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> map = (HashMap<String, Object>) storageTypeData.get(position);
                String path = (String)map.get(storagePath);
                Intent intent = new Intent(MainActivity.this, FilesActivity.class);
                Bundle bundle = new Bundle();
                bundle.putCharSequence("path", path);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }

    private void initData() {
        if (storageTypeData == null) {
            storageTypeData = new ArrayList<>();
        } else {
            storageTypeData.clear();
        }
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            HashMap<String, Object> map = new HashMap<>();
            File file = Environment.getExternalStorageDirectory();
            String name = file.getName();
            StatFs fs = new StatFs(file.getPath());
            long totalStorage = fs.getTotalBytes();
            long usedStorage = totalStorage - fs.getAvailableBytes();
            map.put(storageType, storageTypeSdcard);
            map.put(storagePath, file.getAbsolutePath());
            map.put(storageName, name);
            map.put(storageTotal, totalStorage);
            map.put(storageUsed, usedStorage);
            storageTypeData.add(map);
        }
        File file = new File(sdcardPath);
        if (file.exists()) {
            HashMap<String, Object> map = new HashMap<>();
            String name = file.getName();
            StatFs fs = new StatFs(sdcardPath);
            long totalStorage = fs.getTotalBytes();
            long usedStorage = totalStorage - fs.getAvailableBytes();
            map.put(storageType, storageTypeSdcard);
            map.put(storagePath, file.getAbsolutePath());
            map.put(storageName, name);
            map.put(storageTotal, totalStorage);
            map.put(storageUsed, usedStorage);
            storageTypeData.add(map);
        }
    }
}
