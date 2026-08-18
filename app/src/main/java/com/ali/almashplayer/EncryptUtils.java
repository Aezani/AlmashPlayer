package com.ali.almashplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.crypto.CipherOutputStream;

/**
 * أداة مساعدة لتشفير ملف خام إلى ملف مشفّر باستخدام CryptoUtils (AES/GCM/NoPadding).
 * الناتج يكون:
 * [أول 12 بايت = IV] + [بيانات مشفّرة].
 */
public class EncryptUtils {

    public static void encryptFileToFile(File inFile, File outFile) throws Exception {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        CipherOutputStream cos = null;

        try {
            fis = new FileInputStream(inFile);
            fos = new FileOutputStream(outFile);

            // تحضير Cipher + IV
            byte[] iv = new byte[CryptoUtils.GCM_IV_LENGTH];
            javax.crypto.Cipher cipher = CryptoUtils.createEncryptCipher(iv);

            // كتابة IV في أول الملف الناتج
            fos.write(iv);

            // كتابة البيانات المشفّرة
            cos = new CipherOutputStream(fos, cipher);

            byte[] buffer = new byte[1024 * 256];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, read);
            }
            cos.flush();
        } finally {
            try { if (cos != null) cos.close(); } catch (Exception ignore) {}
            try { if (fos != null) fos.close(); } catch (Exception ignore) {}
            try { if (fis != null) fis.close(); } catch (Exception ignore) {}
        }
    }
}
