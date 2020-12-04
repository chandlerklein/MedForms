package com.myscript.iink.getstarted;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import com.girish.library.buildformer.FormBuilder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;


public class FormActivity extends AppCompatActivity {

    static String templateName;
    static String outDirName;

    private LinearLayout layout;
    private LinkedList<String[]> formValues = new LinkedList<>();
    // private String questionPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        initialize();
    }

    private void initialize() {
        Button btnSubmit = findViewById(R.id.btn_submit);
        templateName = this.getApplicationContext().getResources().getString(R.string.template_file_name);

        // Build the layout from the template retrieved from server in local storage
        String templatePath = this.getApplicationContext().getFilesDir() + File.separator + templateName;
        File templateFile = new File(templatePath);

        String[] missMatches = CreateDocument.compareDOCXToJSON(this.getApplicationContext());

        if (missMatches == null) {
            SftpUtilities.errorString += "• Could not find DOCX template\n";
            ErrorActivity.start(this, "Error – Could not find Word docx", SftpUtilities.errorString);
            return;
        }

        String missingFromDocx = missMatches[0];
        String missingFromJSON = missMatches[1];
        if (missingFromDocx.length() != 0 || missingFromJSON.length() != 0) {
            SftpUtilities.errorString += "Missing from docx template:\n" + missingFromDocx + "\n";
            SftpUtilities.errorString += "Missing from JSON template:\n" + missingFromJSON + "\n";
            ErrorActivity.start(this, "Error — templates do not match:", SftpUtilities.errorString);
            return;
        }

        layout = buildViews(templateFile);
        if (layout != null) {

            if (addQuestionsListeners(layout)) {
                createOutDir();
                // When export button is pressed
                btnSubmit.setOnClickListener(v -> {
                    if (!parseViews(layout)) {
                        SftpUtilities.errorString += "Couldn't get form values or write to file\n";
                    } else {
                        Intent intent = new Intent(FormActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                });
            } else {
                SftpUtilities.errorString += "Couldn't add listeners to the questions\n";
                ErrorActivity.start(this, "Error — verify template name in settings", SftpUtilities.errorString);
            }
        } else {
            SftpUtilities.errorString += "Couldn't get form values or write to file\n";
            ErrorActivity.start(this, "Error — verify template name in settings", SftpUtilities.errorString);
        }
    }

    // Use FormBuilder to add Views to LinearLayout from template file
    private LinearLayout buildViews(File file) {
        LinearLayout layout = findViewById(R.id.linearLayoutQuestions);
        FormBuilder builder = new FormBuilder(this, layout);

        try {
            builder.createFromJson(file);
        } catch (IOException e) {
            e.printStackTrace();
            SftpUtilities.errorString += "Couldn't generate form with FormBuilder\n";
            return null;
        }
        return layout;
    }

    private boolean addQuestionsListeners(final LinearLayout layout) {
        int numViews;
        // Return false if there are no questions in the layout
        if ((numViews = layout.getChildCount()) == 0) {
            SftpUtilities.errorString += "Number of views in layout is 0\n";
            return false;
        }
        // Assign IDs to all the questions
        int idNum = 1;
        for (int i = 0; i < numViews; i++) {
            View view = layout.getChildAt(i);
            if (view instanceof EditText) {
                while (findViewById(idNum) != null) {
                    idNum++;
                }
                view.setId(idNum);

                view.setOnClickListener(v -> {
                    String promptText = null;

                    if (((EditText) v).getHint() != null) {
                        promptText = ((EditText) v).getHint().toString();
                    } else if (layout.getChildAt(layout.indexOfChild(v) - 1) instanceof TextView) {
                        TextView prompt = (TextView) layout.getChildAt(layout.indexOfChild(v) - 1);
                        promptText = prompt.getText().toString();
                    }
                    Intent intent = new Intent(FormActivity.this, IInkEditActivity.class);
                    intent.putExtra("id", String.valueOf(v.getId()));
                    if (promptText != null) {
                        intent.putExtra("windowTitle", promptText);
                    }
                    startActivityForResult(intent, v.getId());
                });
                idNum += 1;
            }
        }
        return true;
    }

    private boolean parseViews(LinearLayout layout) {
        //LinkedList<String[]> formValues = new LinkedList<>();
        int numViews;
        if ((numViews = layout.getChildCount()) == 0) {
            SftpUtilities.errorString += "Number of views in layout is 0\n";
            return false;
        }
        //loop through all views in layout
        for (int i = 0; i < numViews; i++) {
            View view = layout.getChildAt(i);
            String question;
            if (view instanceof EditText) {
                //Edit text with hint rather than text view description
                EditText editText = (EditText) view;
                if(editText.getHint()!= null) {
                    question = editText.getHint().toString();
                }else{
                    Log.d("debug","parseViews is fucked");
                    question = editText.getText().toString();
                }
                String value = editText.getText().toString();
                formValues.add(new String[]{question, value});

            } else if (view instanceof TextView) {
                TextView txtView = (TextView) view;
                question = txtView.getText().toString();

                View nextView = layout.getChildAt(i+1);
                if(nextView == null){
                    continue;
                }
                //check next item in view
                if (nextView instanceof CheckBox) {
                    i++;
                    StringBuilder sb = new StringBuilder();
                    //view2 is seperate from view. At this point view is a TextView preceding
                    //a group of checkboxes
                    View view2 = layout.getChildAt(i);
                    //keep looking at the next view in the layout for checkboxes
                    while (view2 instanceof CheckBox) {
                        CheckBox box = (CheckBox) view2;
                        if (box.isChecked()) {
                            //add text from checkbox to string builder with \n
                            // \n is translated to docx appropriate line break in CreateDocument class
                            sb.append(box.getText().toString()).append(",\n");
                        }
                        i++;
                        view2 = layout.getChildAt(i);
                    }
                    i--;
                    if (sb.length() > 0) {
                        //remove the last comma in the list
                        int commaIndex = sb.lastIndexOf(",");
                        sb.deleteCharAt(commaIndex);
                        String values = sb.toString();
                        formValues.add(new String[]{question, values});
                    } else {
                        // no checkboxes were checked
                        formValues.add(new String[]{question, ""});
                    }

                } else if (nextView instanceof EditText) {
                    //get the text in the edit text following a question prompt
                    i++;
                    EditText editText = (EditText) layout.getChildAt(i);
                    String value = editText.getText().toString();
                    formValues.add(new String[]{question, value});

                } else if (nextView instanceof RadioGroup) {
                    //get the text from the selected radio button following a question prompt
                    i++;
                    RadioGroup group = (RadioGroup) layout.getChildAt(i);
                    int selectedId = group.getCheckedRadioButtonId();
                    RadioButton radioButton = findViewById(selectedId);
                    if (radioButton == null) {
                        formValues.add(new String[]{question, ""});
                        continue;
                    }
                    formValues.add(new String[]{question, radioButton.getText().toString()});
                } else if (nextView instanceof Spinner) {
                    //get the text from the selected option in a dropdown list.
                    i++;
                    Spinner spinner = (Spinner) layout.getChildAt(i);
                    String value = spinner.getSelectedItem().toString();
                    formValues.add(new String[]{question, value});

                }
//                else {
//                    // standalone text view... or the view following the textview is unhandled
//                }
            }
//            else {
//                // single check box, date picker, and time picker. Are unused.
//
//                // single check box is unhandled
//                // date picker is unhandled
//                // time picker is unhandled
//                // Log.d("debugParsing","unhandled standalone view"+ i+ " "+ view.getClass());
//            }
        }
        //write values to text file in case variables in the docx template are wrong
        return writeToFile(formValues);
    }


    // Write form values to a file called output.txt
    private boolean writeToFile(LinkedList<String[]> formValues) {

        // This will create an output.docx if you use a consultation form with variables like ${<text>}
        // that will replace the first few tags with HashMap values in CreateDocument

        CreateDocument.createDocumentFromFormValues(this.getApplicationContext(), formValues);

        // Yikes
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < formValues.size(); i++) {
            String[] values = formValues.get(i);
            if (values[1].equals(" ")) {
                sb.append("\n");
            } else if (values[1].equals("")) {
                String result = values[0] + "\n";
                sb.append(result);
            } else {
                String result = String.format("%s:\n        %s\n", values[0], values[1]);
                sb.append(result);
            }
        }

        // Assign new directory to hold files with name as date format dd-MM-yy'T'HH:mm
        //createOutDir();

        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            // Create file called output.txt in output directory with form values
            File path = new File(this.getFilesDir() + File.separator + outDirName);
            File file = new File(path, "output.txt");
            EncryptedFile encryptedFile = new EncryptedFile.Builder(file, getApplicationContext(), masterKeyAlias, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build();
            FileOutputStream encryptedStream = encryptedFile.openFileOutput();

            encryptedStream.write(sb.toString().getBytes());
            encryptedStream.close();

        } catch (IOException | GeneralSecurityException e) {
            SftpUtilities.errorString += "• Could not write output document";
            ErrorActivity.start(this, "Export error", SftpUtilities.errorString);
        }
        return true;
    }

    private void createOutDir() {
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yy'T'HH:mm", Locale.US);
        outDirName = "session_" + formatter.format(new Date());

        File outDir = new File(this.getFilesDir(), outDirName);

        boolean success = true;
        if (!outDir.exists()) {
            success = outDir.mkdir();
        }
        if (!success) {
            ErrorActivity.start(this, "Could not create folder for documents", SftpUtilities.errorString);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                String contentFromActivity = data.getStringExtra("content");
                String imagePath = data.getStringExtra("imagepath");
                EditText currentView = findViewById(requestCode);
                currentView.setText(contentFromActivity, TextView.BufferType.EDITABLE);
                File tempFile = new File(imagePath);
                String imageName = tempFile.getName();
                String path = tempFile.getPath();
                path = path.replace(imageName,"");
                imageName = imageName.replace(".png", "");
                imageName = imageName+"enc.png";

                //name of encrypted image is the same but with enc in the name.
                File finalFile = new File(path+imageName);
                //encrypt image
                String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                EncryptedFile encryptedFile = new EncryptedFile.Builder(finalFile, getApplicationContext(), masterKeyAlias, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build();
                FileOutputStream encryptedStream = encryptedFile.openFileOutput();
                FileInputStream tempStream = new FileInputStream(tempFile);
                byte[] buf = new byte[8192];
                int length;
                //write contents of tempDoc to encrypted file.
                while ((length = tempStream.read(buf)) > 0) {
                    encryptedStream.write(buf, 0, length);
                }
                tempStream.close();
                //delete temp file.
                tempFile.delete();
                encryptedStream.close();

            }

        } catch (Exception ex) {
            SftpUtilities.errorString += ex.toString() + "\n";
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog alert = new MaterialAlertDialogBuilder(FormActivity.this, R.style.AlertDialogStyle)
                .setTitle(getResources().getString(R.string.warning))
                .setMessage("End consultation?")
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Just close the alert window
                })
                .setPositiveButton("End", (dialog, which) -> {
                    //delete current consultation files.
                    deleteDirectoryOrFile(new File(getApplicationContext().getFilesDir(),outDirName));

                    Intent intent = new Intent(FormActivity.this, MainActivity.class);
                    startActivity(intent);
                })
                .create();
        alert.show();
    }

    private boolean deleteDirectoryOrFile(File dir){
        if(dir.isDirectory()){
            try {
                FileUtils.deleteDirectory(dir);
                return true;
            } catch (IOException e) {
                //could not delete dir
                e.printStackTrace();
                return false;
            }
        }else if(dir.isFile()){
            dir.delete();
            return true;
        }else{
            //something bad is happening
            return false;
        }
    }
}

