// Copyright MyScript. All rights reserved.

package com.myscript.iink.getstarted;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.myscript.iink.Configuration;
import com.myscript.iink.ContentBlock;
import com.myscript.iink.ContentPackage;
import com.myscript.iink.ContentPart;
import com.myscript.iink.ConversionState;
import com.myscript.iink.Editor;
import com.myscript.iink.Engine;
import com.myscript.iink.IEditorListener;
import com.myscript.iink.PackageOpenOption;
import com.myscript.iink.uireferenceimplementation.EditorView;
import com.myscript.iink.uireferenceimplementation.FontUtils;
import com.myscript.iink.uireferenceimplementation.ImageDrawer;
import com.myscript.iink.uireferenceimplementation.InputController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;


public class IInkEditActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "IInkEditActivity";

    private Engine engine;
    private ContentPackage contentPackage;
    private ContentPart contentPart;
    private EditorView editorView;

    private String packageFileName;


    public static String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String baseName = intent.getStringExtra("id");
        this.packageFileName = baseName + ".iink";
        String windowTitle = intent.getStringExtra("windowTitle");
        if (windowTitle == null) {
            setTitle(packageFileName);
        } else {
            setTitle(windowTitle);
        }

        ErrorActivity.installHandler(this);

        engine = IInkApplication.getEngine();

        // configure recognition
        Configuration conf = engine.getConfiguration();
        String confDir = "zip://" + getPackageCodePath() + "!/assets/conf";
        conf.setStringArray("configuration-manager.search-path", new String[]{confDir});

        // Don't need this tmp directory in filesDir?
        // String tempDir = getFilesDir().getPath() + File.separator + "tmp";
        // conf.setString("content-package.temp-folder", tempDir);

        setContentView(R.layout.activity_handwriting);

        editorView = findViewById(R.id.editor_view);

        // load fonts
        AssetManager assetManager = getApplicationContext().getAssets();
        Map<String, Typeface> typefaceMap = FontUtils.loadFontsFromAssets(assetManager);
        editorView.setTypefaces(typefaceMap);

        editorView.setEngine(engine);

        final Editor editor = editorView.getEditor();
        editor.addListener(new IEditorListener() {
            @Override
            public void partChanging(Editor editor, ContentPart oldPart, ContentPart newPart) {
                // no-op
            }

            @Override
            public void partChanged(Editor editor) {
                invalidateOptionsMenu();
                invalidateIconButtons();
            }

            @Override
            public void contentChanged(Editor editor, String[] blockIds) {
                invalidateOptionsMenu();
                invalidateIconButtons();
            }

            @Override
            public void onError(Editor editor, String blockId, String message) {
                Log.e(TAG, "Failed to edit block \"" + blockId + "\"" + message);
            }
        });

        setInputMode(InputController.INPUT_MODE_FORCE_PEN); // If using an active pen, put INPUT_MODE_AUTO here


        File file = new File(getFilesDir() + "/" + FormActivity.outDirName, packageFileName);
        try {
            contentPackage = engine.openPackage(file, PackageOpenOption.CREATE);
            // Choose type of content (possible values are: "Text Document", "Text", "Diagram", "Math", "Drawing" and "Raw Content")
            int partCount = contentPackage.getPartCount();

            if (partCount == 0) { //no parts
                contentPart = contentPackage.createPart("Text"); // Choose type of content (possible values are: "Text Document", "Text", "Diagram", "Math", and "Drawing")
            } else {
                contentPart = contentPackage.getPart(contentPackage.getPartCount() - 1);
            }

        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to open package \"" + packageFileName + "\"", e);
        }

        //setTitle("Type: " + contentPart.getType());

        // wait for view size initialization before setting part
        editorView.post(new Runnable() {
            @Override
            public void run() {
                editorView.getRenderer().setViewOffset(0, 0);
                editorView.getRenderer().setViewScale(1);
                editorView.setVisibility(View.VISIBLE);
                editor.setPart(contentPart);
            }
        });

        findViewById(R.id.button_input_mode_forcePen).setOnClickListener(this);
        findViewById(R.id.button_input_mode_forceTouch).setOnClickListener(this);
        findViewById(R.id.button_input_mode_auto).setOnClickListener(this);
        findViewById(R.id.button_undo).setOnClickListener(this);
        findViewById(R.id.button_redo).setOnClickListener(this);
        findViewById(R.id.button_clear).setOnClickListener(this);

        invalidateIconButtons();
    }

    @Override
    protected void onDestroy() {
        editorView.setOnTouchListener(null);
        editorView.close();

        if (contentPart != null) {
            contentPart.close();
            contentPart = null;
        }
        if (contentPackage != null) {
            contentPackage.close();
            contentPackage = null;
        }


        // IInkApplication has the ownership, do not close here
        engine = null;

        super.onDestroy();
    }

    protected final boolean savePackage() {
        if (contentPart == null)
            return false;
        try {
            //saveHistory();
            contentPart.getPackage().save();
            //storeState();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save package", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //do save before calling superclass
            saveActivityData();

        }
        //any other keycode
        return super.onKeyDown(keyCode, event);
    }

    protected void saveActivityData() {
        ContentBlock block = editorView.getEditor().getRootBlock();
        String pathToTextFile = export_(block, ".txt");
        String pathToImage = export_(block, ".png");

        Intent intent = getIntent();
        //intent.putExtra("filePath", pathToTextFile);
        try {
            String stuff = getStringFromFile(pathToTextFile);
            File temp = new File(pathToTextFile);
            temp.delete();
            intent.putExtra("content", stuff);
            intent.putExtra("imagepath", pathToImage);
            setResult(RESULT_OK, intent);
            finish();

        } catch (Exception e) {
            e.printStackTrace();
        }
        savePackage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);

        MenuItem convertMenuItem = menu.findItem(R.id.menu_convert);
        convertMenuItem.setEnabled(true);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_convert: {
                Editor editor = editorView.getEditor();
                ConversionState[] supportedStates = editor.getSupportedTargetConversionStates(null);
                if (supportedStates.length > 0)
                    editor.convert(null, supportedStates[0]);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_input_mode_forcePen:
                setInputMode(InputController.INPUT_MODE_FORCE_PEN);
                break;
            case R.id.button_input_mode_forceTouch:
                setInputMode(InputController.INPUT_MODE_FORCE_TOUCH);
                break;
            case R.id.button_input_mode_auto:
                setInputMode(InputController.INPUT_MODE_AUTO);
                break;
            case R.id.button_undo:
                editorView.getEditor().undo();
                break;
            case R.id.button_redo:
                editorView.getEditor().redo();
                break;
            case R.id.button_clear:
                editorView.getEditor().clear();
                break;
            default:
                Log.e(TAG, "Failed to handle click event");
                break;
        }
    }

    private void setInputMode(int inputMode) {
        editorView.setInputMode(inputMode);
        findViewById(R.id.button_input_mode_forcePen).setEnabled(inputMode != InputController.INPUT_MODE_FORCE_PEN);
        findViewById(R.id.button_input_mode_forceTouch).setEnabled(inputMode != InputController.INPUT_MODE_FORCE_TOUCH);
        findViewById(R.id.button_input_mode_auto).setEnabled(inputMode != InputController.INPUT_MODE_AUTO);
    }

    private void invalidateIconButtons() {
        Editor editor = editorView.getEditor();
        final boolean canUndo = editor.canUndo();
        final boolean canRedo = editor.canRedo();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageButton imageButtonUndo = (ImageButton) findViewById(R.id.button_undo);
                imageButtonUndo.setEnabled(canUndo);
                ImageButton imageButtonRedo = (ImageButton) findViewById(R.id.button_redo);
                imageButtonRedo.setEnabled(canRedo);
                ImageButton imageButtonClear = (ImageButton) findViewById(R.id.button_clear);
                imageButtonClear.setEnabled(contentPart != null);
            }
        });
    }

    private String export_(final ContentBlock block, final String fileExtension) {


        String filename = this.packageFileName;
        int dotPos = filename.lastIndexOf('.');
        String basename = dotPos > 0 ? filename.substring(0, dotPos) : filename;
        String finalFileName = basename + fileExtension;
        File txtFile = new File(getFilesDir() + "/" + FormActivity.outDirName, finalFileName);

        if (txtFile != null) {
            try {
                ImageDrawer imageDrawer = new ImageDrawer();
                imageDrawer.setImageLoader(editorView.getImageLoader());
                editorView.getEditor().waitForIdle();
                editorView.getEditor().export_(block, txtFile.getPath(), imageDrawer);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to export file", Toast.LENGTH_LONG).show();
            }
        }

        return txtFile.getPath();
    }
}



