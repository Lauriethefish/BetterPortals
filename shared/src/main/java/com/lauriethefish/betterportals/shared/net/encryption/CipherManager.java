package com.lauriethefish.betterportals.shared.net.encryption;

import com.google.inject.Singleton;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Utility to creating a cipher based on a {@link UUID}. This is convenient for configuration, although it's not great practise.
 * This is symmetric encryption.
 */
@Singleton
public class CipherManager {
    private static final int AES_KEY_SIZE = 256; // Bits
    private static final int GCM_NONCE_LENGTH = 12; // Bytes
    private static final int GCM_TAG_LENGTH = 16; // Bytes

    private SecretKey secretKey;

    private GCMParameterSpec spec;

    /**
     * Initialises the secret key based on <code>key</code>.
     * @param key The key to base the encryption key on.
     * @throws NoSuchAlgorithmException If the encryption algorithm wasn't found - this shouldn't happen in practise
     */
    public void init(UUID key) throws NoSuchAlgorithmException  {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(uuidToBytes(key));

        // Create our IV from random bytes with the correct block size
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        random.nextBytes(nonce);
        spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);

        // Generate a new 256 bit AES key from our UUID
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE, random);
        secretKey = keyGenerator.generateKey();
    }

    private byte[] uuidToBytes(UUID id)    {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(id.getMostSignificantBits());
        buffer.putLong(id.getLeastSignificantBits());
        return buffer.array();
    }

    public Cipher createEncrypt() throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        return cipher;
    }

    public Cipher createDecrypt() throws GeneralSecurityException  {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        return cipher;
    }
}
