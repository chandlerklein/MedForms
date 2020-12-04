package com.myscript.iink.getstarted;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.GeneralSecurityException;
import java.util.Objects;

public class ConfigurationMenuActivity extends AppCompatActivity {
    static final String MY_PREFERENCES = "MyPrefs";
    static final String TEMPLATE_KEY = "templateKey";
    static final String DOCX_TEMPLATE_KEY = "docxTemplateKey";
    static final String EXPORT_KEY = "exportKey";
    static final String IP_KEY = "ipKey";
    static final String PORT_KEY = "portKey";
    static final String SSH_KEY = "sshKey";
    static final String USER_KEY = "usernameKey";
    static final String PASSWORD_KEY = "passwordKey";

    private static SharedPreferences sharedPreferences;

    private final int REQUEST_CODE = 1;

    private EditText editTxtTemplate;
    private EditText editTxtDocxTemplate;
    private EditText editTxtExport;
    private EditText editTxtIp;
    private EditText editTxtPort;
    private EditText editTxtUsername;
    private EditText editTxtSsh;
    private EditText editTxtPassword;

    private Button btnSave;
    private Button btnCancel;
    private Button btnQr;

    private String originalTemplate;
    private String originalDocxTemplate;
    private String originalExport;
    private String originalIp;
    private String originalPort;
    private String originalSsh;
    private String originalUsername;
    private String originalPassword;

    // return the SharedPreferences object to wherever it's requested from
    static SharedPreferences getSharedPreferences(WeakReference<Context> context) {
        String masterKeyAlias = null;
        try {
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(context.get(), "Could not get masterKeyAlias", Toast.LENGTH_SHORT).show();
        }
        try {
            assert masterKeyAlias != null;
            return EncryptedSharedPreferences.create(
                    MY_PREFERENCES,
                    masterKeyAlias,
                    context.get(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(context.get(), "Could not get preferences", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration_menu);
        if (!initFields()) {
            Toast.makeText(this, "Fields could not be initialized", Toast.LENGTH_SHORT).show();
        } else {
            setBtnListeners();
        }
    }

    private boolean initFields() {
        WeakReference<Context> cReference = new WeakReference<>(getApplicationContext());
        if ((sharedPreferences = getSharedPreferences(cReference)) == null) {
            return false;
        }

        editTxtTemplate = findViewById(R.id.editTxtTemplate);
        editTxtDocxTemplate = findViewById(R.id.editTxtdocxTemplate);
        editTxtExport = findViewById(R.id.editTxtExport);
        editTxtIp = findViewById(R.id.editTxtIp);
        editTxtPort = findViewById(R.id.editTxtPort);
        editTxtSsh = findViewById(R.id.editTxtSsh);
        editTxtUsername = findViewById(R.id.editTxtUsername);
        editTxtPassword = findViewById(R.id.editTxtPassword);

        editTxtTemplate.setText(sharedPreferences.getString(TEMPLATE_KEY, ""));
        editTxtDocxTemplate.setText(sharedPreferences.getString(DOCX_TEMPLATE_KEY, ""));
        editTxtExport.setText(sharedPreferences.getString(EXPORT_KEY, ""));
        editTxtIp.setText(sharedPreferences.getString(IP_KEY, ""));
        editTxtPort.setText(sharedPreferences.getString(PORT_KEY, ""));
        editTxtSsh.setText(sharedPreferences.getString(SSH_KEY, ""));
        editTxtUsername.setText(sharedPreferences.getString(USER_KEY, ""));
        editTxtPassword.setText(sharedPreferences.getString(PASSWORD_KEY, ""));

        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnQr = findViewById(R.id.btnQr);

        originalTemplate = editTxtTemplate.getText().toString();
        originalDocxTemplate = editTxtDocxTemplate.getText().toString();
        originalExport = editTxtExport.getText().toString();
        originalIp = editTxtIp.getText().toString();
        originalPort = editTxtPort.getText().toString();
        originalSsh = editTxtSsh.getText().toString();
        originalUsername = editTxtUsername.getText().toString();
        originalPassword = editTxtPassword.getText().toString();

        return true;
    }

    private void setBtnListeners() {
        // when save button is pressed
        btnSave.setOnClickListener(v -> {
            // if all of the EditText values remained the same, don't save
            if (isUnchanged()) {
                Toast.makeText(ConfigurationMenuActivity.this, "Change a value first to save", Toast.LENGTH_SHORT).show();
            } else {
                if (isIncomplete()) {
                    Toast.makeText(ConfigurationMenuActivity.this, "Please finish filling out the form", Toast.LENGTH_SHORT).show();
                } else {
                    if (saveChanges()) {
                        Intent intent = new Intent(ConfigurationMenuActivity.this, MainActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(ConfigurationMenuActivity.this, "Could not save changes", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // when cancel button is pressed
        btnCancel.setOnClickListener(v -> {
            // if all of the EditText values remained the same, just go back to main menu
            if (isIncomplete() && isUnchanged()) {
                sharedPreferences = null;
                Intent intent = new Intent(ConfigurationMenuActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                if (isUnchanged()) {
                    Intent intent = new Intent(ConfigurationMenuActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    showWarningAlert();
                }
            }
        });

        // when 'Use QR Code Reader' is pressed
        btnQr.setOnClickListener(v -> {
            Intent intent = new Intent(this, QrActivity.class);
            startActivityForResult(new Intent(intent), REQUEST_CODE);
        });
    }

    // replace the SSH key field with the value from the QR Activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                assert data != null;
                String returnedResult = Objects.requireNonNull(data.getData()).toString();
                editTxtSsh.setText(returnedResult);
                saveChanges();
            }
        }
    }

    // save the form with the values that are currently in the fields
    private boolean saveChanges() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TEMPLATE_KEY, editTxtTemplate.getText().toString());
        editor.putString(DOCX_TEMPLATE_KEY, editTxtDocxTemplate.getText().toString());
        editor.putString(EXPORT_KEY, editTxtExport.getText().toString());
        editor.putString(IP_KEY, editTxtIp.getText().toString());
        editor.putString(PORT_KEY, editTxtPort.getText().toString());
        editor.putString(SSH_KEY, editTxtSsh.getText().toString());
        editor.putString(USER_KEY, editTxtUsername.getText().toString());
        editor.putString(PASSWORD_KEY, editTxtPassword.getText().toString());
        return editor.commit();
    }

    // warning if user wants to discard form changes
    private void showWarningAlert() {
        AlertDialog alert = new MaterialAlertDialogBuilder(ConfigurationMenuActivity.this, R.style.AlertDialogStyle)
                .setTitle(getResources().getString(R.string.warning))
                .setMessage("Discard changes?")
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Just close the warning box
                })
                .setPositiveButton("Discard", (dialog, which) -> {
                    setOriginalValues();
                    if (isIncomplete()) {
                        sharedPreferences = null;
                        Intent intent = new Intent(ConfigurationMenuActivity.this, MainActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(ConfigurationMenuActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                })
                .create();
        alert.show();
    }

    // if changes discarded, use values originally in form
    private void setOriginalValues() {
        editTxtTemplate.setText(originalTemplate);
        editTxtDocxTemplate.setText(originalDocxTemplate);
        editTxtExport.setText(originalExport);
        editTxtIp.setText(originalIp);
        editTxtPort.setText(originalPort);
        editTxtSsh.setText(originalSsh);
        editTxtUsername.setText(originalUsername);
        editTxtPassword.setText(originalPassword);
    }

    // return true if any of the form fields are empty
    private boolean isIncomplete() {
        return editTxtTemplate.getText().toString().equals("")
                || editTxtExport.getText().toString().equals("")
                || editTxtDocxTemplate.getText().toString().equals("")
                || editTxtIp.getText().toString().equals("")
                || editTxtPort.getText().toString().equals("")
                || editTxtSsh.getText().toString().equals("")
                || editTxtUsername.getText().toString().equals("")
                || editTxtPassword.getText().toString().equals("");
    }

    // return true if all the form fields are the same as the original version
    private boolean isUnchanged() {
        return editTxtTemplate.getText().toString().equals(originalTemplate)
                && editTxtExport.getText().toString().equals(originalExport)
                && editTxtDocxTemplate.getText().toString().equals(originalDocxTemplate)
                && editTxtIp.getText().toString().equals(originalIp)
                && editTxtPort.getText().toString().equals(originalPort)
                && editTxtSsh.getText().toString().equals(originalSsh)
                && editTxtUsername.getText().toString().equals(originalUsername)
                && editTxtPassword.getText().toString().equals(originalPassword);
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }
}