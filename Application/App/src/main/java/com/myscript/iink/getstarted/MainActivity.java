package com.myscript.iink.getstarted;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private Button btnStart;
    private Button btnDocs;
    private Button btnConfig;
    private Button btnTransfer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFields();
        setBtnListeners();
    }

    private void initFields() {
        btnStart = findViewById(R.id.btnStart);
        btnDocs = findViewById(R.id.btnDocs);
        btnConfig = findViewById(R.id.btnConfig);
        btnTransfer = findViewById(R.id.btnTransfer);
    }

    private void setBtnListeners() {
        // when 'Start Consultation Form' button is pressed

        // create alert listener for download choice
        final DialogInterface.OnClickListener alertListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                startFormActivity();
            } else if (which == DialogInterface.BUTTON_POSITIVE) {
                if (downloadTemplate()) {
                    startFormActivity();
                }
            }
        };

        // Start Consultation Form Button
        btnStart.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences(ConfigurationMenuActivity.MY_PREFERENCES, Context.MODE_PRIVATE);
            if (sharedPreferences.getAll().size() <= 2) {
                Snackbar.make(v, "Please fill out configuration form first", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Ok", v1 -> {
                            // Dismiss SnackBar
                        }).setActionTextColor(getColor(R.color.gray))
                        .show();
            } else {
                // get template file based on its @string value
                File jsonFile = new File(getApplicationContext().getFilesDir(), getApplicationContext().getResources().getString(R.string.template_file_name));
                File docxFile = new File(getApplicationContext().getFilesDir(), getApplicationContext().getResources().getString(R.string.docx_template_file_name));
                if (jsonFile.exists() && docxFile.exists()) {

                    // see if user wants to update the template file
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext(), R.style.AlertDialogStyle);
                    builder.setTitle("System");
                    builder.setMessage("Do you want to generate the form with the file on the tablet or get a new file from the server?");
                    builder.setPositiveButton("Get file from server", alertListener);
                    builder.setNegativeButton("Use file on tablet", alertListener);
                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    // file does not exist in memory — just download file and start FormActivity
                    downloadTemplate();
                    startFormActivity();
                }
            }
        });

        // when 'Documentation' button is pressed
        btnDocs.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });

        // when 'Configuration' button is pressed
        btnConfig.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ConfigurationMenuActivity.class);
            startActivity(intent);
        });

        // when 'Transfer files' button is pressed
        btnTransfer.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences(ConfigurationMenuActivity.MY_PREFERENCES, Context.MODE_PRIVATE);

            if (sharedPreferences.getAll().size() <= 2) {
                Snackbar.make(v, "Please fill out configuration form first", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Ok", v1 -> {
                            // Dismiss SnackBar
                        }).setActionTextColor(getColor(R.color.gray))
                        .show();
            } else {
                Intent intent = new Intent(MainActivity.this, FileTransferMenuActivity.class);
                startActivity(intent);
            }
        });
    }

    // returns true on success
    private boolean downloadTemplate() {
        WeakReference<Context> cReference = new WeakReference<>(getApplicationContext());
        try {
            Object fetchResult = new ServerTask(cReference).execute(ServerTask.GET_FILE).get();
            if (fetchResult == null) {
                ErrorActivity.start(this, "Error — verify configuration menu settings", SftpUtilities.errorString);
                return false;
            } else {
                return true;
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void startFormActivity() {
        // at this point FormActivity.templateName should already be set
        Intent intent = new Intent(MainActivity.this, FormActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }


}
