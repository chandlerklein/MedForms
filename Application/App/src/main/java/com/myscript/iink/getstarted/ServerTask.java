package com.myscript.iink.getstarted;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Vector;

public class ServerTask extends AsyncTask<String, Void, Object> {

    static final String GET_FILE = "GET_FILE";
    static final String PUT_FILE = "PUT_FILE";
    static final String LIST_FILE = "LIST_FILE";

    static final int SUCCESS = 0;
    static final Object FAILURE = null;

    private WeakReference<Context> mContext;
    private File outputFile;

    public ServerTask(WeakReference<Context> mContext) {
        this.mContext = mContext;
    }

    /**
     * @param mContext   Context of the activity that created this task
     * @param outputFile The directory/file that is being sent to the server
     */
    public ServerTask(WeakReference<Context> mContext, File outputFile) {
        this.mContext = mContext;
        this.outputFile = outputFile;
    }

    /**
     * This method performs the user specified server operation — get a template from the server,
     * put a file on the server, or list the files on the server.
     *
     * @param requestType RequestType can be GET_FILE, PUT_FILE, or LIST_FILE.
     * @return On success, GET_FILE returns 0, PUT_FILE returns 0, and LIST_FILE returns
     * Vector<ChannelSftp.LsEntry>. On failure, return null.
     */
    @Override
    protected Object doInBackground(String... requestType) {
        try {
            // Get the SharedPreferences object using context passed into ServerTask
            SharedPreferences sp = ConfigurationMenuActivity.getSharedPreferences(mContext);

            // Get path of the template file from SharedPreferences
            assert sp != null;
            String templateName = sp.getString(ConfigurationMenuActivity.TEMPLATE_KEY, "");

            // Get path of docx template file from SharedPreferences
            String docxTemplateName = sp.getString(ConfigurationMenuActivity.DOCX_TEMPLATE_KEY, "");

            // Get path of output directory from SharedPreferences
            String exportDirectory = sp.getString(ConfigurationMenuActivity.EXPORT_KEY, "");

            // Get remote host IP address from SharedPreferences
            String hostAddress = sp.getString(ConfigurationMenuActivity.IP_KEY, "");

            // Get port number from SharedPreferences
            int port = Integer.parseInt(Objects.requireNonNull(sp.getString(ConfigurationMenuActivity.PORT_KEY, "22")));

            // Get username for remote host from SharedPreferences
            String username = sp.getString(ConfigurationMenuActivity.USER_KEY, "");

            // Get password for remote host from SharedPreferences
            String password = sp.getString(ConfigurationMenuActivity.PASSWORD_KEY, "");

            // Get decodedKey
            String rawKey = sp.getString(ConfigurationMenuActivity.SSH_KEY, "");
            HostKey hostKey = SftpUtilities.decodeKey(hostAddress, rawKey);

            // Add key to known hosts
            JSch jsch = new JSch();
            jsch.getHostKeyRepository().add(hostKey, null);

            // Channel connect the session using JSch, username, password, IP address, port number
            Session session = SftpUtilities.createSession(jsch, username, password, hostAddress, port);
            assert session != null;
            SftpUtilities.sessionConnect(session);
            ChannelSftp sftpChannel = SftpUtilities.channelConnect(session);
            assert sftpChannel != null;

            switch (requestType[0]) {
                case GET_FILE:
                    try {
                        // Specify file to grab from the server using SharedPreferences template name
                        InputStream inStream = sftpChannel.get(templateName);

                        // Copy the input stream to a file with the same name as the server file
                        File file = new File(mContext.get().getFilesDir() + File.separator + mContext.get().getResources().getString(R.string.template_file_name));
                        SftpUtilities.copyInputStreamToFile(inStream, file);

                        //Do the same for docx template file
                        inStream = sftpChannel.get(docxTemplateName);
                        file = new File(mContext.get().getFilesDir() + File.separator + mContext.get().getResources().getString(R.string.docx_template_file_name));
                        SftpUtilities.copyInputStreamToFile(inStream, file);

                        // Close channel and session
                        sftpChannel.disconnect();
                        session.disconnect();
                        return SUCCESS;
                    } catch (SftpException e) {
                        // Close channel and session
                        sftpChannel.disconnect();
                        session.disconnect();

                        SftpUtilities.errorString += "• File Not Found\n• Check the json and docx import file path in the config menu";
                        e.printStackTrace();
                        return FAILURE;
                    }

                case PUT_FILE:
                    try {
                        //trying to send an unencrypted file breaks everything currently
                        String dirPath = exportDirectory + File.separator + outputFile.getName();
                        if (outputFile.isDirectory()) {

                            // Create new server directory for the files
                            sftpChannel.mkdir(dirPath);

                            for (File file : Objects.requireNonNull(outputFile.listFiles())) {
                                String fname = FilenameUtils.getExtension(file.getName());
                                if (fname.compareTo("iink") == 0) {
                                    file.delete();
                                    continue;
                                }

                                String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                                EncryptedFile encryptedFile = new EncryptedFile.Builder(file, mContext.get(), masterKeyAlias, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build();
                                FileInputStream is = encryptedFile.openFileInput();

                                boolean condition = false;
                                try {
                                    is.read();
                                    is.close();
                                    condition = true;
                                } catch (IOException e) {
                                    //file wasn't encrypted
                                    is.close();
                                    // open regular input stream
                                    FileInputStream nonEncryptedFile = new FileInputStream(file);
                                    sftpChannel.put(nonEncryptedFile, dirPath + File.separator + file.getName());
                                    nonEncryptedFile.close();
                                }

                                if (condition) {
                                    //file is encrypted. open FileInput stream again
                                    is = encryptedFile.openFileInput();
                                    sftpChannel.put(is, dirPath + File.separator + file.getName());
                                    is.close();
                                }
                            }
                        } else {
                            // If the file isn't a directory just put it on the server
                            // Use SharedPreferences export location plus the file name to send file
                            //InputStream is = new FileInputStream(outputFile);
                            File file = outputFile;
//                            String fname = FilenameUtils.getExtension(file.getName());
//                            if (fname.compareTo("iink") == 0) {
//                                file.delete();
//                                break;
//                            }

                            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                            EncryptedFile encryptedFile = new EncryptedFile.Builder(file, mContext.get(), masterKeyAlias, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build();
                            FileInputStream is = encryptedFile.openFileInput();

                            boolean condition = false;
                            try {
                                is.read();
                                is.close();
                                condition = true;
                            } catch (IOException e) {
                                //file wasn't encrypted
                                is.close();
                                // open regular input stream
                                FileInputStream nonEncryptedFile = new FileInputStream(file);
                                sftpChannel.put(nonEncryptedFile, dirPath + File.separator + file.getName());
                                nonEncryptedFile.close();
                            }

                            if (condition) {
                                //file is encrypted. open FileInput stream again
                                is = encryptedFile.openFileInput();
                                sftpChannel.put(is, dirPath + File.separator + file.getName());
                                is.close();
                            }
                        }

                        // Close channel and session
                        sftpChannel.disconnect();
                        session.disconnect();
                        return SUCCESS;
                    } catch (SftpException e) {
                        // Close channel and session
                        sftpChannel.disconnect();
                        session.disconnect();

                        SftpUtilities.errorString += "• Could not write to directory";
                        e.printStackTrace();
                        return FAILURE;
                    }

                case LIST_FILE:
                    try {
                        assert templateName != null;
                        String templateDir = templateName.substring(0, templateName.lastIndexOf('/') + 1);

                        // OLD: mContext.get().getResources().getString(R.string.server_template_directory)
                        // Get the Vector list of files from the template directory on the server
                        @SuppressWarnings("unchecked")
                        Vector<ChannelSftp.LsEntry> filesList = sftpChannel.ls(templateDir);

                        @SuppressWarnings("unchecked")
                        Vector<ChannelSftp.LsEntry> exportList = sftpChannel.ls(exportDirectory);

                        // Add list of exported documents to list of templates
                        filesList.addAll(exportList);

                        // Close channel and session
                        sftpChannel.disconnect();
                        session.disconnect();

                        // Return Vector on success
                        return filesList;
                    } catch (SftpException e) {
                        // Close channel and session
                        sftpChannel.disconnect();
                        session.disconnect();
                        SftpUtilities.errorString += "• Could not find files to list";
                        e.printStackTrace();
                        return FAILURE;
                    }

                default:
                    return FAILURE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return FAILURE;
        }
    }
}

// Put each of the files in the new directory
//                            for (File file : Objects.requireNonNull(outputFile.listFiles())) {
//                                String fname = FilenameUtils.getExtension(file.getName());
//                                if (fname.compareTo("iink") == 0) {
//                                    file.delete();
//                                    continue;
//                                }
//
//                                String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
//                                EncryptedFile encryptedFile = new EncryptedFile.Builder(file, mContext.get(), masterKeyAlias, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build();
//                                FileInputStream is = encryptedFile.openFileInput();
//
//
//                                File tempFile = new File(mContext.get().getFilesDir() + File.separator + file.getName());
//                                FileOutputStream os = new FileOutputStream(tempFile);
//                                byte[] buf = new byte[8192];
//                                int length;
//                                boolean condition = false;
//                                try {
//                                    while ((length = is.read(buf)) > 0) {
//                                        os.write(buf, 0, length);
//                                    }
//                                    os.close();
//                                    is.close();
//                                    condition = true;
//                                } catch (IOException e) {
//                                    //file wasn't encrypted
//                                    is.close();
//                                    os.close();
//                                    tempFile.delete();
//                                    FileInputStream nonEncryptedFile = new FileInputStream(file);
//                                    sftpChannel.put(nonEncryptedFile, dirPath + File.separator + file.getName());
//                                    nonEncryptedFile.close();
//                                }
//
//                                if (condition) {
//                                    //now we have an unencrypted temp file.
//                                    //open tempFile inputstream to transfer to server.
//                                    FileInputStream tempInput = new FileInputStream(tempFile);
//                                    sftpChannel.put(tempInput, dirPath + File.separator + file.getName());
//                                    tempInput.close();
//                                    tempFile.delete();
//                                }
//                            }
