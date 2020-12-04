package com.myscript.iink.getstarted;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Properties;

public class SftpUtilities {

    static final int CONNECTION_TIMEOUT = 10000;
    static String errorString = "";

    protected static HostKey decodeKey(String host, String rawKey) {
        try {
            // The rawKey comes from /etc/ssh/ssh_host_rsa_key.pub on the host.
            byte[] key = Base64.getDecoder().decode(rawKey);

            return new HostKey(host, key);
        } catch (JSchException ex) {
            errorString += "• Bad host IP address or SSH key\n";
            ex.printStackTrace();
            return null;
        }
    }

    protected static Session createSession(JSch jsch, String username, String password, String host, int port) {
        Session session;
        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "yes");
            session.setConfig(prop);
            return session;
        } catch (JSchException e) {
            errorString += "• Could not create SSH session — verify IP address and SSH key\n";
            e.printStackTrace();
            return null;
        }
    }

    protected static void sessionConnect(Session session) {
        try {
            session.connect(CONNECTION_TIMEOUT);
        } catch (JSchException e) {
            errorString += "• Couldn't connect — verify IP address and key\n";
            e.printStackTrace();
        }
    }

    protected static ChannelSftp channelConnect(Session session) {
        try {
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(CONNECTION_TIMEOUT);
            return sftpChannel;
        } catch (JSchException e) {
            errorString += "• SFTP connection channel error\n";
            e.printStackTrace();
            return null;
        }
    }

    protected static void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorString += "• Could not write to file\n";
        } finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if (out != null) {
                    out.close();
                }
                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            } catch (Exception e) {
                errorString += "• Could not write to file\n";
                e.printStackTrace();
            }
        }
    }
}
