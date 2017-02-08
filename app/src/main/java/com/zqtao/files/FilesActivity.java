package com.zqtao.files;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.zqtao.db.FilesData;
import com.zqtao.db.ThumbnailData;
import com.zqtao.service.ThumbnailService;
import com.zqtao.util.FilesOperation;
import com.zqtao.util.MediaFile;
import com.zqtao.util.SearchRunnable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FilesActivity extends AppCompatActivity{

    static public final String logTag = "Files";

    static public final int VIEW_STATUS_NORMAL = 0;
    static public final int VIEW_STATUS_DELETE = 1;
    static public final int VIEW_STATUS_COPY = 2;
    static public final int VIEW_STATUS_MOVE = 3;
    static public final int VIEW_STATUS_SEARCH = 4;

    static public final String strFileName = "fileName";
    static public final String strFilePath = "filePath";
    static public final String strFileIsFolder = "isFolder";
    static public final String strFileSelected = "fileSelectedStatus";
    static public final String strFileTime = "fileTime";
    static public final String strFileThumbnail = "fileThumbnail";
    static public final String strFileTimeFormat = "yyyy-MM-dd HH:mm:ss";

    private RelativeLayout pathNavigation;
    private Handler handler;
    private HorizontalScrollView scrollView;
    private ListView fileListView;
    private FilesAdapter filesAdapter;
    private Toolbar toolbar;
    private MenuItem searchViewItem;

    private List<String> folderNameNavigation;
    private List<Map<String, Object>> fileInfo;
    private List<String> selectedFiles;
    private String currentPath;
    private String pathBackup;
    private int createType;
    private boolean isSelectedAll;
    private int viewStatus = VIEW_STATUS_NORMAL; /* 0 -- normal, 1 -- copy, 2 -- move */
    private boolean isShowHiddenFile = false;

    private FilesData filesData;
    private ThumbnailHandler thumbnailHandler;

    class ThumbnailHandler extends Handler {
        public ThumbnailHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (filesData != null) {
                        ThumbnailData thumbnailData = (ThumbnailData) msg.obj;
                        filesData.updateThumbnail(thumbnailData.getFilePath(), thumbnailData.getThumbnailPath());
                        FilesAdapter adapter = (FilesAdapter) fileListView.getAdapter();
                        adapter.updateFileType(thumbnailData.getFilePath(), thumbnailData.getThumbnailPath());
                    }
                    break;
                case 1:
                    FilesAdapter adapter = (FilesAdapter) fileListView.getAdapter();
                    fileInfo.add((Map<String,Object>)msg.obj);
                    adapter.notifyDataSetChanged();
                    break;
                default:
                    Log.e(logTag, "message.what type is error!");
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thumbnailHandler = new ThumbnailHandler(Looper.getMainLooper());
        ThumbnailService.setMainHandler(thumbnailHandler);

        setContentView(R.layout.activity_files);
        if (toolbar == null) {
            toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.back);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (filesAdapter.getEditMode()) {
                        isSelectedAll = !isSelectedAll;
                        selectedAll(isSelectedAll);
                        setTitleString(true);
                    } else {
                        onBackPressed();
                    }
                }
            });
        }
        setTitleString(false);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setItems(new String[]{"Create Folder", "Create File"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                createType = which;
                                createNewFile();
                            }
                        });
                builder.show();
            }
        });
        if (currentPath == null) {
            Intent intent = getIntent();
            Bundle  bundle = intent.getExtras();
            String path = bundle.getString("path");
            if (path == null) {
                currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            } else {
                currentPath = path;
            }
        }

        if (scrollView == null) {
            scrollView = (HorizontalScrollView) findViewById(R.id.path_scroll);
        }
        if (handler == null) {
            handler = new Handler();
        }

        if (fileListView == null) {
            fileListView = (ListView) findViewById(R.id.file_list);
        }

        initData();
        initFolderNavigationData();
        initFolderNavigationView();

        if (filesAdapter == null) {
            filesAdapter = new FilesAdapter(this, fileInfo);
        }
        fileListView.setAdapter(filesAdapter);
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> map = (HashMap<String, Object>) fileInfo.get(position);
                String filePath = (String) map.get(strFilePath);
                if (filesAdapter.getEditMode()) {
                    boolean isSelected = !(boolean) map.get(strFileSelected);
                    map.put(strFileSelected, isSelected);
                    FilesAdapter.HolderView holderView = (FilesAdapter.HolderView) view.getTag();
                    holderView.selected.setChecked(isSelected);
                    putSelectedFile(filePath, isSelected);
                    setTitleString(true);
                    return;
                }
                File file = new File(filePath);
                if (file.isDirectory()) {
                    currentPath = filePath;
                    /* Update file list */
                    updateView();

                    /* Update folder navigation */
                    if (viewStatus == VIEW_STATUS_SEARCH) {
                        viewStatus = VIEW_STATUS_NORMAL;
                        initFolderNavigationData();
                        initFolderNavigationView();
                        return;
                    } else {
                        String fileName = (String) (map.get("fileName"));
                        folderNameNavigation.add(fileName);
                        addFolderNavigationButton(fileName, folderNameNavigation.size());
                    }
                } else {
                    launchApp(filePath);
                }
            }
        });

        fileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (filesAdapter.getEditMode()) {
                    return true;
                }
                if (selectedFiles == null) {
                    selectedFiles = new ArrayList<>();
                }
                filesAdapter.setEditMode(true);
                filesAdapter.notifyDataSetChanged();

                FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
                fab.setVisibility(View.GONE);

                setTitleString(true);
                invalidateOptionsMenu();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        searchViewItem = null;
        if (filesAdapter.getEditMode()) {
            getMenuInflater().inflate(R.menu.edit_menu, menu);
        } else {
            switch (viewStatus) {
                case VIEW_STATUS_NORMAL:
                    getMenuInflater().inflate(R.menu.menu_files, menu);
                    searchViewItem = menu.findItem(R.id.action_search);

                    setSearchView();
                    break;
                case VIEW_STATUS_COPY:
                    getMenuInflater().inflate(R.menu.copy_menu, menu);
                    MenuItem moveItem = menu.findItem(R.id.button_move);
                    moveItem.setVisible(false);
                    break;
                case VIEW_STATUS_MOVE:
                    getMenuInflater().inflate(R.menu.copy_menu, menu);
                    MenuItem pasteItem = menu.findItem(R.id.button_paste);
                    pasteItem.setVisible(false);
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item =  menu.findItem(R.id.action_show_hidden);
        if (item != null) {
            if (isShowHiddenFile) {
                item.setTitle(R.string.action_hide_hidden);
            } else {
                item.setTitle(R.string.action_show_hidden);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_delete:
                Log.i(logTag, String.valueOf(selectedFiles.size()));
                if (selectedFiles != null && selectedFiles.size() > 0) {
                    FilesOperation filesOperation = new FilesOperation(this, selectedFiles, VIEW_STATUS_DELETE);
                    filesOperation.execute();
                }
                break;
            case R.id.action_rename:
                if (selectedFiles != null && selectedFiles.size() == 1) {
                    newName();
                }
                break;
            case R.id.action_copy:
                createCopyView(VIEW_STATUS_COPY);
                break;
            case R.id.action_move:
                createCopyView(VIEW_STATUS_MOVE);
                break;
            case R.id.action_share:
                launchSharePanel();
                break;
            case R.id.button_cancel:
                backwardFromCopy();
                break;
            case R.id.button_paste:
                if (selectedFiles != null && selectedFiles.size() > 0) {
                    FilesOperation filesOperation = new FilesOperation(this, selectedFiles, VIEW_STATUS_COPY);
                    filesOperation.setCopyPath(currentPath);
                    filesOperation.execute();
                }
                break;
            case R.id.button_move:
                if (selectedFiles != null && selectedFiles.size() > 0) {
                    FilesOperation filesOperation = new FilesOperation(this, selectedFiles, VIEW_STATUS_MOVE);
                    filesOperation.setCopyPath(currentPath);
                    filesOperation.execute();
                }
                break;
            case R.id.action_show_hidden:
                isShowHiddenFile = !isShowHiddenFile;
                updateView();
                break;
            default:
                Log.i(logTag, "Action Type is error!");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (searchViewItem != null && searchViewItem.isActionViewExpanded()) {
            searchViewItem.collapseActionView();
            return;
        }
        if (viewStatus == VIEW_STATUS_SEARCH) {
            viewStatus = VIEW_STATUS_NORMAL;
            updateView();
            return;
        }
        if (filesAdapter.getEditMode()) {
            backwardFromEdit(false);
            return;
        }
        if ((viewStatus == VIEW_STATUS_COPY || viewStatus == VIEW_STATUS_MOVE) &&
                currentPath.equals("/")) {
            backwardFromCopy();
        } else {
            if (currentPath.equals("/")) {
                super.onBackPressed();
            } else {
                File file = new File(currentPath);
                /* Update file list view */
                currentPath = file.getParent();
                updateView();

                /* Update folder navigation */
                removeOneButton();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, ThumbnailService.class));
    }

    private FilesActivity getActivity() {
        return this;
    }

    /* Get all of file name in current path */
    private void initData() {
        if (fileInfo == null) {
            fileInfo = new ArrayList<>();
        } else {
            fileInfo.clear();
        }
        if (filesData == null) {
            filesData = new FilesData(this);
        }
        File dirFile = new File(currentPath);
        if (dirFile.isDirectory()) {
            File[] files = dirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!isShowHiddenFile) {
                        char startC = file.getName().charAt(0);
                        if (startC == '.') {
                            continue;
                        }
                    }
                    String filePath = file.getAbsolutePath();
                    Map<String, Object> map = new HashMap<>();
                    map.put(strFileName, file.getName());
                    map.put(strFilePath, filePath);
                    map.put(strFileIsFolder, file.isDirectory());
                    map.put(strFileSelected, false);
                    /* Set file's latest modify time */
                    SimpleDateFormat sdf = new SimpleDateFormat(strFileTimeFormat, Locale.CHINA);
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(file.lastModified());
                    map.put(strFileTime, sdf.format(cal.getTime()));

                    if (MediaFile.isImageFileType(filePath) || MediaFile.isVideoFileType(filePath)) {
                        /* 从数据库获取thumbnail，并保存 */
                        String thumbnailPath = filesData.getThumbnail(filePath);;
                        if (thumbnailPath == null || thumbnailPath.isEmpty()) {
                            Intent intent = new Intent(this, ThumbnailService.class);
                            Bundle bundle = new Bundle();
                            bundle.putCharSequence(MainActivity.imagePath, filePath);
                            intent.putExtras(bundle);
                            startService(intent);
                        } else {
                            map.put(FilesActivity.strFileThumbnail, thumbnailPath);
                        }
                    }
                    fileInfo.add(map);
                }
            }
        }
    }

    public void updateView() {
        initData();
        filesAdapter.notifyDataSetInvalidated();
    }

    public void backwardFromEdit(boolean isUpdateData) {
        boolean isUpdatePathNv = false;
        if (viewStatus != VIEW_STATUS_NORMAL) {
            currentPath = pathBackup;
            pathBackup = null;
            viewStatus = VIEW_STATUS_NORMAL;
            isUpdatePathNv = true;
        }
        if (isUpdateData) {
            initData();
        }
        filesAdapter.setEditMode(false);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);

        selectedAll(false);
        setTitleString(false);
        invalidateOptionsMenu();

        selectedFiles.clear();
        if (isUpdatePathNv) {
            initFolderNavigationData();
            initFolderNavigationView();
        }
    }

    private void initFolderNavigationData() {
        if (folderNameNavigation == null) {
            folderNameNavigation = new ArrayList<>();
        } else {
            folderNameNavigation.clear();
        }
        folderNameNavigation.add("/");
        int start = 1;
        int end = currentPath.indexOf('/', start);
        while (end != -1) {
            String folderName = currentPath.substring(start, end);
            folderNameNavigation.add(folderName);
            start = end+1;
            end = currentPath.indexOf('/', start);
        }
        folderNameNavigation.add(currentPath.substring(start));
    }

    private void initFolderNavigationView() {
        pathNavigation = (RelativeLayout)findViewById(R.id.path_navigation);
        pathNavigation.removeAllViews();

        for (int i = 0; i < folderNameNavigation.size(); ++i) {
            addFolderNavigationButton(folderNameNavigation.get(i), i+1);
        }
    }

    private void addFolderNavigationButton(String buttonName, int id) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.path_button, null);
        view.setId(id);
        if (id == 1) {
            ImageView image = (ImageView)view.findViewById(R.id.path_arrow);
            image.setVisibility(View.GONE);
        }
        Button button = (Button)view.findViewById(R.id.path_button);
        button.setText(buttonName);
        button.setTag(id);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filesAdapter.getEditMode()) {
                    return;
                }
                int id = (int) v.getTag();
                for (int i = folderNameNavigation.size(); i > id; --i) {
                    removeOneButton();
                }
                currentPath = "";
                for (int i = 0; i < folderNameNavigation.size(); ++i) {
                    if (i == 0) {
                        currentPath += folderNameNavigation.get(i);
                    } else {
                        currentPath += "/";
                        currentPath += folderNameNavigation.get(i);
                    }
                }
                updateView();
            }
        });
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        if (id == 1) {
            lp.addRule(RelativeLayout.ALIGN_PARENT_START);
        } else {
            lp.addRule(RelativeLayout.RIGHT_OF, id-1);
        }
        pathNavigation.addView(view, lp);

        handler.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_RIGHT);

            }
        });
    }

    private void removeOneButton() {
        folderNameNavigation.remove(folderNameNavigation.size() - 1);
        pathNavigation.removeViewAt(pathNavigation.getChildCount() - 1);
    }

    private void createNewFile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (createType == 0) {
            builder.setTitle("Create Folder");
        } else {
            builder.setTitle("Create File");
        }
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.create_file, null);
        builder.setView(layout);
        final EditText editText = (EditText)layout.findViewById(R.id.new_file_name);
        builder.setPositiveButton(R.string.button_create, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = currentPath + "/" + editText.getText().toString();
                File file = new File(newName);
                if (file.exists()) {
                    if (createType == 0) {
                        Toast.makeText(getActivity(), "New folder is exist, please rename again!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "New file is exist, please rename again!", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                try {
                    if (createType == 0) {
                        if (file.mkdir()) {
                            Log.i(logTag, "Create folder success!");
                        }
                    } else {
                        if (file.createNewFile()) {
                            Log.i(logTag, "Create file success!");
                        }
                    }
                    updateView();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    private void newName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_rename);
        builder.setIcon(R.drawable.rename);

        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.create_file, null);
        builder.setView(layout);
        final EditText editText = (EditText)layout.findViewById(R.id.new_file_name);
        HashMap<String, Object> map = getSelectedItem();
        String name = (String)map.get(strFileName);
        editText.setText(name, TextView.BufferType.EDITABLE);
        editText.setSelection(name.length());
        builder.setPositiveButton(R.string.button_done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = currentPath + "/" + editText.getText().toString();
                File newFile = new File(newName);
                if (newFile.exists()) {
                    Toast.makeText(getActivity(), "File is exist, please rename again!", Toast.LENGTH_SHORT).show();
                    return;
                }
                HashMap<String, Object> map = getSelectedItem();
                String path = (String) map.get(strFilePath);
                File file = new File(path);
                if (file.renameTo(newFile)) {
                    Log.i(logTag, "Rename successfully!");
                    backwardFromEdit(true);
                } else {
                    Log.i(logTag, "Rename failed!");
                }
            }
        });

        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    private void createCopyView(int operationType) {
        filesAdapter.setEditMode(false);
        viewStatus = operationType;
        pathBackup = currentPath;
        currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        updateView();
        invalidateOptionsMenu();
        setTitleString(false);

        initFolderNavigationData();
        initFolderNavigationView();
    }

    private void launchSharePanel() {
        ArrayList<Uri> myList = new ArrayList<>();
        String mimeType = "";
        boolean needGetMime = true;
        for(int i=0; i < selectedFiles.size(); i++){
            String filePath = selectedFiles.get(i);
            if (needGetMime) {
                String fileMimeType = getMimeType(filePath);
                if (mimeType.isEmpty()) {
                    mimeType = fileMimeType;
                } else if (fileMimeType.compareTo(mimeType) != 0) {
                    mimeType = "*/*";
                    needGetMime = false;
                }
            }
            Uri uri = Uri.fromFile(new File(filePath));
            myList.add(uri);
        }

        Intent intent = new Intent(myList.size() > 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
        intent.setType(mimeType);
        if (myList.size() > 1) {
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, myList);
        } else {
            intent.putExtra(Intent.EXTRA_STREAM, myList.get(0));
        }
        startActivity(Intent.createChooser(intent, "Share"));
    }

    private void backwardFromCopy() {
        viewStatus = 0;
        currentPath = pathBackup;
        pathBackup = null;
        updateView();
        invalidateOptionsMenu();

        initFolderNavigationData();
        initFolderNavigationView();
    }
    public void selectedAll(boolean isSelected) {
        for (int i = 0; i < fileInfo.size(); ++i) {
            HashMap<String, Object> map = (HashMap<String, Object>)fileInfo.get(i);
            map.put(strFileSelected, isSelected);
            putSelectedFile((String)map.get(strFilePath), isSelected);
        }

        filesAdapter.notifyDataSetChanged();
    }

    public void putSelectedFile(String path, boolean isSelected) {
        if (isSelected) {
            if (!selectedFiles.contains(path)) {
                selectedFiles.add(path);
            }
        } else {
            selectedFiles.remove(path);
        }
    }

    private HashMap<String, Object> getSelectedItem() {
        HashMap<String, Object> map;
        for (int i = 0; i < fileInfo.size(); ++i) {
            map = (HashMap<String, Object>)fileInfo.get(i);
            if ((boolean)map.get(strFileSelected)) {
                return map;
            }
        }
        return null;
    }

    public void setTitleString(boolean isEditMode) {
        if (!isEditMode) {
            toolbar.setNavigationIcon(R.drawable.back);
            toolbar.setTitle(R.string.app_name);
            return;
        }
        int count = selectedFiles.size();
        if (count == fileInfo.size()) {
            toolbar.setNavigationIcon(R.drawable.check);
            toolbar.setTitle(String.valueOf(count));
        } else {
            toolbar.setNavigationIcon(R.drawable.uncheck);
            if (0 == count) {
                toolbar.setTitle(R.string.action_select_items);
            } else {
                toolbar.setTitle(String.valueOf(count));
            }
        }
    }

    public FilesData getFilesData() {
        return filesData;
    }

    public static String getMimeType(String path) {
        String type = null;
        String extension = path.substring(path.lastIndexOf('.')+1, path.length());
        if (!extension.isEmpty()) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private void launchApp(String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("file://" + path);
        intent.setDataAndType(uri, getMimeType(path));
        startActivity(intent);
    }

    private void setSearchView() {
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchViewItem);
        searchViewItem.collapseActionView();
        MenuItemCompat.setOnActionExpandListener(searchViewItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchViewItem);
                searchView.setIconified(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });

        searchView.setIconified(true);
        searchView.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS);
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchViewItem);
                searchView.setQuery("", false);
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchViewItem);
                searchView.setQuery("", false);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                searchViewItem.collapseActionView();
                viewStatus = VIEW_STATUS_SEARCH;

                if (fileInfo != null) {
                    fileInfo.clear();
                }

                SearchRunnable searchRunnable = new SearchRunnable();
                searchRunnable.setCurrentPath(currentPath);
                searchRunnable.setSearchKey(query);
                searchRunnable.setHandler(thumbnailHandler);
                searchRunnable.setCheckHiddenFile(isShowHiddenFile);
                Thread thread = new Thread(searchRunnable);
                thread.start();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
    }
}
