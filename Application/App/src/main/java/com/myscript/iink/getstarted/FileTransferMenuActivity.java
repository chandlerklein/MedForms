package com.myscript.iink.getstarted;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class FileTransferMenuActivity extends AppCompatActivity {

    private Button btnBack;
    private LinearLayout linearLayoutServerFiles;
    private LinearLayout linearLayoutLocalFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_transfer_menu);

        initFields();
        setServerFileList();
        setLocalFileList();
        setBtnListeners();
    }

    private void initFields() {
        btnBack = findViewById(R.id.btnBack);
        linearLayoutServerFiles = findViewById(R.id.linearLayoutServerFiles);
        linearLayoutLocalFiles = findViewById(R.id.linearLayoutLocalFiles);
    }

    private void setServerFileList() {
        WeakReference<Context> cReference = new WeakReference<>(getApplicationContext());
        try {
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> fileList = (Vector<ChannelSftp.LsEntry>) new ServerTask(cReference).execute(ServerTask.LIST_FILE).get();
            if (fileList != null) {
                // Sort the list of files by date modified
                Comparator<ChannelSftp.LsEntry> fileComparator = (e1, e2) -> e1.getAttrs().getMtimeString().compareTo(e2.getAttrs().getMtimeString());
                fileList = fileList.stream().sorted(fileComparator).collect(Collectors.toCollection(Vector::new));

                fileList.forEach(entry -> {
                    // Ignore '.' and '..' when listing files on the server
                    if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                        TextView tv = new TextView(this);
                        SftpATTRS attrs = entry.getAttrs();
                        String fileDesc = attrs.getMtimeString() + "\n" + "<b>" + entry.getFilename() + "</b>";
                        tv.setText(Html.fromHtml(fileDesc, Build.VERSION.SDK_INT));
                        tv.setGravity(Gravity.START);
                        tv.setPadding(0, 0, 0, 50);
                        tv.setTextSize(20);
                        linearLayoutServerFiles.addView(tv);
                    }
                });
            } else {
                ErrorActivity.start(this, "Error — verify configuration menu settings", SftpUtilities.errorString);
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            SftpUtilities.errorString += "Could not list server files";
            ErrorActivity.start(this, "Error — verify configuration menu settings", SftpUtilities.errorString);
        }
    }

    private void setLocalFileList() {
        String path = this.getFilesDir().toString();
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files != null) {
            // Sort the list of files by date modified
            Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));

            for (File file : files) {
                TextView tv = new TextView(this);
                Date lastModDate = new Date(file.lastModified());
                String fileDesc = lastModDate.toString() + "\n" + "<b>" + file.getName() + "</b>";
                tv.setText(Html.fromHtml(fileDesc, Build.VERSION.SDK_INT));
                tv.setGravity(Gravity.START);
                tv.setPadding(0, 0, 0, 50);
                tv.setTextSize(20);

                tv.setOnClickListener(v -> {
                    // Change selected file background to green
                    v.setBackgroundColor(this.getColor(R.color.green));
                    AlertDialog alert = new MaterialAlertDialogBuilder(this, R.style.AlertDialogStyle)
                            .setTitle(getResources().getString(R.string.warning))
                            .setMessage("Are you sure you want to transfer '" + file.getName() + "' to the server? The file will be deleted after it is transferred.")
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                // Just close the warning box and change color back to white from green
                                v.setBackgroundColor(getApplicationContext().getColor(R.color.white));
                            })
                            .setPositiveButton("Transfer", (dialog, which) -> {
                                try {
                                    WeakReference<Context> cReference = new WeakReference<>(getApplicationContext());
                                    //Context context = this.getApplicationContext();
                                    // Send selected directory or file to the server
                                    Object result = new ServerTask(cReference, file).execute(ServerTask.PUT_FILE).get();
                                    if(result!=null) {
                                        //if result is not null then the operation was successful
                                        // If dir/file was successfully deleted from the file directory
                                        if (deleteRecursive(file)) {
                                            // Take the local file off the list
                                            linearLayoutLocalFiles.removeView(v);

                                            // Reload the server file list, which should have the file transferred now
                                            linearLayoutServerFiles.removeAllViews();
                                            setServerFileList();
                                        } else {
                                            ErrorActivity.start(this, "Error", "• Could not delete file/directory");
                                        }
                                    }else{
                                        //server task failed
                                        ErrorActivity.start(this, "File could not be transfered", SftpUtilities.errorString);
                                    }

                                } catch (InterruptedException | ExecutionException e) {
                                    ErrorActivity.start(this, "Error — verify configuration menu settings", SftpUtilities.errorString);
                                }
                            })
                            .create();
                    alert.show();
                });
                linearLayoutLocalFiles.addView(tv);
            }
        }
    }

    // Remove the file or directory and all files inside
    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles())) {
                deleteRecursive(child);
            }
        }
        return fileOrDirectory.delete();
    }

    private void setBtnListeners() {

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(FileTransferMenuActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        // Do nothing
    }
}