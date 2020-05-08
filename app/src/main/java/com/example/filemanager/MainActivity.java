package com.example.filemanager;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout1);
    }

    private boolean isFileManagerInitialized = false;
    private boolean[] selection;
    private File[] files;
    private List<String> fileList;
    private int numberFilesFound;
    private Button refreshButton;
    private File dir;
    private String currentPath;
    private boolean selectionClick;
    private int selectedItemIndex;
    private String copyingPath;


    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISIONS);
            numberFilesFound = files.length;
            return;
        }
        if (!isFileManagerInitialized) {

            currentPath = String.valueOf(Environment.
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            final String rootPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
            final TextView pathOutput = findViewById(R.id.pathOutput);
            final ListView listView = findViewById(R.id.listView);
            final TextAdapter textAdapter1 = new TextAdapter();
            listView.setAdapter(textAdapter1);
            fileList = new ArrayList<>();

            refreshButton = findViewById(R.id.refresh);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/') + 1));
                    dir = new File(currentPath);
                    files = dir.listFiles();
                    numberFilesFound = files.length;
                    selection = new boolean[numberFilesFound];
                    textAdapter1.setSelection(selection);
                    fileList.clear();
                    for (int i = 0; i < numberFilesFound; i++) {
                        fileList.add(String.valueOf(files[i].getAbsolutePath()));
                    }
                    textAdapter1.setData(fileList);

                }
            });
            refreshButton.callOnClick();

            final Button goBackButton = findViewById(R.id.goBack);
            goBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPath.equals(rootPath)) {
                        return;
                    }
                    currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
                    refreshButton.callOnClick();
                }
            });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!selectionClick) {
                                if (files[position].isDirectory()) {
                                    currentPath = files[position].getAbsolutePath();
                                    refreshButton.callOnClick();
                                }
                            }
                        }
                    }, 50);
                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    selectionClick = true;
                    selection[position] = !selection[position];
                    textAdapter1.setSelection(selection);
                    int selectionCount = 0;
                    for (boolean aSelection : selection) {
                        if (aSelection) {
                            selectionCount++;
                        }
                    }
                    if (selectionCount > 0) {
                        if (selectionCount == 1) {
                            selectedItemIndex = position;
                            findViewById(R.id.rename).setVisibility(View.VISIBLE);
                            if (!files[selectedItemIndex].isDirectory()) {
                                findViewById(R.id.copy).setVisibility(View.VISIBLE);
                            }
                        } else {
                            findViewById(R.id.rename).setVisibility(View.GONE);
                            findViewById(R.id.copy).setVisibility(View.GONE);
                        }
                        findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.bottomBar).setVisibility(View.GONE);
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            selectionClick = false;
                        }
                    }, 1000);
                    return false;
                }
            });

            final Button del = findViewById(R.id.del);


            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete");
                    deleteDialog.setMessage("Are you sure you want to delete file?");
                    deleteDialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (int i = 0; i < files.length; i++) {
                                if (selection[i]) {
                                    deleteFileOrFolder(files[i]);
                                    selection[i] = false;
                                }
                            }
                            refreshButton.callOnClick();
                        }

                    });
                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            refreshButton.callOnClick();
                        }
                    });
                    deleteDialog.show();
                }
            });

            final Button createNewFolder = findViewById(R.id.newFolder);

            createNewFolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder newFolderDialog =
                            new AlertDialog.Builder(MainActivity.this);
                    newFolderDialog.setTitle("Enter New Folder Name");
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    newFolderDialog.setView(input);
                    newFolderDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final File newFolder = new File(currentPath + "/" + input.getText());
                            if (!newFolder.exists()) {
                                newFolder.mkdir();
                                refreshButton.callOnClick();
                            }

                        }
                    });
                    newFolderDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    newFolderDialog.show();
                }
            });

            final Button renameButton = findViewById(R.id.rename);
            renameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder renameDialog =
                            new AlertDialog.Builder(MainActivity.this);
                    renameDialog.setTitle("Rename file?");
                    final EditText input = new EditText(MainActivity.this);
                    final String newnamePath = files[selectedItemIndex].getAbsolutePath();
                    input.setText(newnamePath.substring(newnamePath.lastIndexOf('/')));
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    renameDialog.setView(input);
                    renameDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String s = new File(newnamePath).getParent() + "/" + input.getText();
                            File newFile = new File(s);
                            new File(newnamePath).renameTo(newFile);
                            refreshButton.callOnClick();
                            selection = new boolean[files.length];
                            textAdapter1.setSelection(selection);
                        }
                    });
                    renameDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    refreshButton.callOnClick();
                                }
                            });
                    renameDialog.show();
                }
            });

            final Button copyButton = findViewById(R.id.copy);
            copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    copyingPath = files[selectedItemIndex].getAbsolutePath();
                    selection = new boolean[files.length];
                    textAdapter1.setSelection(selection);
                    findViewById(R.id.paste).setVisibility(View.VISIBLE);
                }
            });

            final Button pasteButton = findViewById(R.id.paste);
            pasteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pasteButton.setVisibility(View.GONE);
                    String destinationPath = currentPath + copyingPath.substring(copyingPath.lastIndexOf('/'));
                    copy(new File(copyingPath), new File(destinationPath));
                    refreshButton.callOnClick();
                }
            });

            isFileManagerInitialized = true;
        } else {
            refreshButton.callOnClick();
        }
    }

    private void copy(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    class TextAdapter extends BaseAdapter {

        private List<String> data = new ArrayList<>();

        private boolean[] selection;

        public void setData(List<String> data) {
            if (data != null) {
                this.data.clear();
                if (data.size() > 0) {
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }

        }

        void setSelection(boolean[] selection) {
            if (selection != null) {
                this.selection = new boolean[selection.length];
                for (int i = 0; i < selection.length; i++) {
                    this.selection[i] = selection[i];
                }
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.textItem)));
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = getItem(position);
            holder.info.setText(item.substring(item.lastIndexOf('/') + 1));
            if (selection != null) {
                if (selection[position]) {
                    holder.info.setBackgroundColor(Color.argb(100, 9, 9, 9));
                } else {
                    holder.info.setBackgroundColor(Color.WHITE);
                }
            }
            return convertView;
        }

        class ViewHolder {
            TextView info;

            ViewHolder(TextView info) {
                this.info = info;
            }

        }
    }

    private static final int REQUEST_PERMISIONS = 1234;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PERMISSIONS_COUNT = 2;

    @SuppressLint("NewApi")
    private boolean arePermissionsDenied() {
        int p = 0;
        while (p < PERMISSIONS_COUNT) {
            if (checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            p++;
        }

        return false;
    }


    private void deleteFileOrFolder(File fileOrFolder) {
        if (fileOrFolder.isDirectory()) {
            if (fileOrFolder.list().length == 0) {
                fileOrFolder.delete();
            } else {
                String files[] = fileOrFolder.list();
                for (String temp : files) {
                    File fileToDelete = new File(fileOrFolder, temp);
                    deleteFileOrFolder(fileToDelete);
                }
                if (fileOrFolder.list().length == 0) {
                    fileOrFolder.delete();
                }
            }
        } else {
            fileOrFolder.delete();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISIONS && grantResults.length > 0) {
            if (arePermissionsDenied()) {
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            } else {
                onResume();
            }
        }
    }

}

