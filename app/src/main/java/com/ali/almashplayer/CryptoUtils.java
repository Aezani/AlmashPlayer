package com.ali.almashplayer;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * أدوات بسيطة للتعامل مع مفتاح AES مخزَّن في Android Keystore
 * واستخدامه مع AES/GCM/NoPadding.
 */
public class CryptoUtils {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "almash_aes_video_key";
    public static final String AES_MODE = "AES/GCM/NoPadding";
    public static final int GCM_IV_LENGTH = 12;   // 12 bytes IV
    public static final int GCM_TAG_LENGTH = 128; // bits

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        SecretKey existingKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        if (existingKey != null) {
            return existingKey;
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE);
        keyGenerator.init(
                new android.security.keystore.KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                                | android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
        );
        return keyGenerator.generateKey();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static Cipher createEncryptCipher(byte[] ivOut) throws Exception {
        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        // GCM يولّد IV تلقائياً
        byte[] iv = cipher.getIV();
        System.arraycopy(iv, 0, ivOut, 0, iv.length);
        return cipher;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static Cipher createDecryptCipher(byte[] iv) throws Exception {
        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance(AES_MODE);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher;
    }
}
