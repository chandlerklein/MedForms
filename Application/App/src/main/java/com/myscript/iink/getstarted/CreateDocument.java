package com.myscript.iink.getstarted;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.girish.library.buildformer.model.BFCheckbox;
import com.girish.library.buildformer.model.BFCheckboxGroup;
import com.girish.library.buildformer.model.BFDropDownList;
import com.girish.library.buildformer.model.BFEditText;
import com.girish.library.buildformer.model.BFRadioGroup;
import com.girish.library.buildformer.model.BFRadioGroupRatings;
import com.girish.library.buildformer.model.BFView;
import com.girish.library.buildformer.model.JSONFeed;
import com.girish.library.buildformer.utils.Constants;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.myscript.iink.getstarted.FormActivity.templateName;

public class CreateDocument {
    // private static ObjectFactory objectFactory = new ObjectFactory();

    public static void createDocumentFromFormValues(Context context, LinkedList<String[]> formValues) {
        // String path = context.getFilesDir() + File.separator + FormActivity.outDirName + File.separator + "newDoc.docx";
        try {
            File file = new File(context.getFilesDir() + File.separator + context.getResources().getString(R.string.docx_template_file_name));

            WordprocessingMLPackage wordprocessingMLPackage = WordprocessingMLPackage.load(file);
            MainDocumentPart documentPart = wordprocessingMLPackage.getMainDocumentPart();
            List<String> tagValues = getTagValues(documentPart.getXML());

            for (String tagValue : tagValues) {
                Log.d("debugTest", tagValue);
            }

            // we can make our own HashMap with responses from the form to replace the Word docx variables
            HashMap<String, String> mappings = new HashMap<>();
            // assign the values directly from the form values
            for (String[] arr : formValues) {

                if (arr[1].contains("\n")) {
                    //replace \n with the equivalent for xml
                    mappings.put(arr[0], newlineToBreakHack(arr[1]));
                } else {
                    mappings.put(arr[0], arr[1]);
                }
            }
            mappings.entrySet().forEach(entry->{
                System.out.println(entry.getKey() + " " + entry.getValue());
            });

            // this replaces each ${<text>} in the Word doc with the
            // value for the key <text> from the HashMap
            documentPart.variableReplace(mappings);

            // save output document
            String outputDocumentPath = context.getFilesDir() + File.separator + FormActivity.outDirName + File.separator + "output.docx";
            File outDoc = new File(outputDocumentPath);

            // tempDoc is an unencrypted file for wordprocessingMLPackage to call save on before it is encrypted to the file outDoc.
            File tempDoc = new File(context.getFilesDir() + File.separator + "temp.docx");
            wordprocessingMLPackage.save(tempDoc);
            FileInputStream tempStream = new FileInputStream(tempDoc);

            // encrypt docx output file
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            //create encrypted file
            EncryptedFile encryptedFile = new EncryptedFile.Builder(outDoc, context, masterKeyAlias, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build();
            FileOutputStream encryptedStream = encryptedFile.openFileOutput();

            byte[] buf = new byte[8192];
            int length;

            // write contents of tempDoc to encrypted file
            while ((length = tempStream.read(buf)) > 0) {
                encryptedStream.write(buf, 0, length);
            }
            tempStream.close();

            // delete temp file, make sure it was deleted
            if (!tempDoc.delete()) {
                Toast.makeText(context, "Couldn't remove temp file", Toast.LENGTH_SHORT).show();
            }
            encryptedStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> getTagValues(final String str) {
        final List<String> tagValues = new ArrayList<>();

        // ignore dumb redundant character warning
        //noinspection RegExpRedundantEscape
        Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(str);

        while (matcher.find()) {
            tagValues.add(matcher.group());
        }
        return tagValues;
    }

    public static String[] compareDOCXToJSON(Context context) {
        try {
            String path = context.getFilesDir() + File.separator + context.getResources().getString(R.string.docx_template_file_name);
            File file = new File(path);
            if (!file.exists()) {
                return null;
            }
            WordprocessingMLPackage wordprocessingMLPackage = WordprocessingMLPackage.load(file);
            MainDocumentPart documentPart = wordprocessingMLPackage.getMainDocumentPart();
            List<String> tagValues = getTagValues(documentPart.getXML());

            //get list of tags not found in docx file.
            return checkTags(context, tagValues);
        } catch (Exception e) {
            return null;
        }
    }

    //leading or trailing whitespace is still an issue.
    public static String[] checkTags(Context context, List<String> tagValues) throws IOException {
        LinkedList<String> templateValues = new LinkedList<>();

        String templatePath = context.getFilesDir() + File.separator + templateName;
        File templateFile = new File(templatePath);

        ObjectMapper mapper = new ObjectMapper();
        JSONFeed feed = mapper.readValue(templateFile, JSONFeed.class);
        List<BFView> views = feed.getViews();

        for (BFView view : views) {
            String type = view.getType();
            switch (type) {
                case Constants.TYPE_TEXT_VIEW:
                    // Not used
                    //BFTextView bfTextView = (BFTextView) view;
                    //createTextView(bfTextView.getText(), bfTextView.getTextSize());
                    break;
                case Constants.TYPE_EDIT_TEXT:
                    BFEditText bfEditText = (BFEditText) view;
                    templateValues.add(bfEditText.getDescription());
                    break;
                case Constants.TYPE_CHECKBOX:
                    BFCheckbox bfCheckbox = (BFCheckbox) view;
                    templateValues.add(bfCheckbox.getDescription());
                    // createCheckbox(bfCheckbox.getDescription());
                    break;
                case Constants.TYPE_CHECKBOX_GROUP:
                    BFCheckboxGroup bfCheckboxGroup = (BFCheckboxGroup) view;
                    templateValues.add(bfCheckboxGroup.getDescription());
                    break;
                case Constants.TYPE_RADIO_GROUP:
                    BFRadioGroup bfRadioGroup = (BFRadioGroup) view;
                    templateValues.add(bfRadioGroup.getDescription());
                    break;
                case Constants.TYPE_RADIO_GROUP_RATINGS:
                    BFRadioGroupRatings bfRadioGroupRatings = (BFRadioGroupRatings) view;
                    templateValues.add(bfRadioGroupRatings.getDescription());
                    break;
                case Constants.TYPE_DROP_DOWN_LIST:
                    BFDropDownList bfDropDownList = (BFDropDownList) view;
                    templateValues.add(bfDropDownList.getDescription());
                    break;
                case Constants.TYPE_SWITCH:
                    //not used
                    //BFSwitch bfSwitch = (BFSwitch) view;
                    //createSwitch(bfSwitch.getDescription(), bfSwitch.getFirstChoice(), bfSwitch.getSecondChoice());
                    break;
                case Constants.TYPE_DATE_PICKER:
                    //not used
                    //createDatePicker();
                    break;
                case Constants.TYPE_TIME_PICKER:
                    // not used
                    //createTimePicker();
                    break;
                case Constants.TYPE_SECTION_BREAK:
                    //not used
                    //createSectionBreak();
                    break;
            }
        }
        //remove ${ and } from the tag values
        for (int i = 0; i < tagValues.size(); i++) {
            //noinspection RegExpRedundantEscape
            tagValues.set(i, tagValues.get(i).replaceAll("[\\$\\{\\}]", ""));
        }

        StringBuilder docxSb = new StringBuilder();
        // docxsb.append("Tags not in docx:");

        StringBuilder jsonSb = new StringBuilder();
        //jsonsb.append("Tags not in JSON:");

        //tagValues has the variable from the docx template
        //templateValues has the description from all the json template objects

        //check that docx has all the variables from json
        for (String value : templateValues) {
            if (!tagValues.contains(value)) {
                docxSb.append("\"").append(value).append("\", ");
            }
        }
        //check that json has all the variables from docx
        for (String value : tagValues) {
            if (!templateValues.contains(value)) {
                jsonSb.append("\"").append(value).append("\", ");
            }
        }
        int startindex, lastindex;
        if (docxSb.length() > 0) {
            startindex = docxSb.lastIndexOf(",");
            lastindex = docxSb.lastIndexOf(" ");
            docxSb.delete(startindex, lastindex);
        }
        if (jsonSb.length() > 0) {
            startindex = jsonSb.lastIndexOf(",");
            lastindex = jsonSb.lastIndexOf(" ");
            jsonSb.delete(startindex, lastindex);
        }
        String[] returnString = new String[2];
        returnString[0] = docxSb.toString();
        returnString[1] = jsonSb.toString();

//        for (String value : templateValues){
//            Log.d("debugTest", value);
//        }
        return returnString;
    }

    private static String newlineToBreakHack(String r) {

        StringTokenizer st = new StringTokenizer(r, "\n\r\f"); // tokenize on the newline character, the carriage-return character, and the form-feed character
        StringBuilder sb = new StringBuilder();

        boolean firsttoken = true;
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (firsttoken) {
                firsttoken = false;
            } else {
                sb.append("</w:t><w:br/><w:t>");
            }
            sb.append(line);
        }
        return sb.toString();
    }
}
