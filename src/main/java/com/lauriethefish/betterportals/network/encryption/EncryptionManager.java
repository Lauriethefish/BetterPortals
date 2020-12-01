package com.lauriethefish.betterportals.network.encryption;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// Class to make using AES encryption easy, using a UUID as the key
// Yes, I know that this likely isn't totally secure
public class EncryptionManager  {
    public static final int AES_KEY_SIZE = 256; // Bits
    public static final int GCM_NONCE_LENGTH = 12; // Bytes
    public static final int GCM_TAG_LENGTH = 16; // Bytes

    private SecretKey secretKey;

    private GCMParameterSpec spec;
    private byte[] nonce;

    // Makes a new encryption manager using a UUID as its key
    public EncryptionManager(UUID key) throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG"); // Make sure the SecureRandom algorithm is always the same
        random.setSeed(uuidToBytes(key));

        // Create our IV from random bytes with the correct block size
        nonce = new byte[GCM_NONCE_LENGTH]; random.nextBytes(nonce);
        spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        
        // Generate a new 256 bit AES key from our random
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE, random);
        secretKey = keyGenerator.generateKey();
    }

    // Converts a 128 bit UUID to a 16 byte array
    private byte[] uuidToBytes(UUID id)    {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(id.getMostSignificantBits());
        buffer.putLong(id.getLeastSignificantBits());
        return buffer.array();
    }

    // Gets a new Cipher instance using the specified mode and the key in the EncryptionManager
    // The mode specifies whether this Cipher will be used for encryption or decryption.
    public Cipher newCipherInstance(int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // Create a new cipher instance
        cipher.init(mode, secretKey, spec); // Set it up with our key, mode and algorithm.

        return cipher;
    }
}