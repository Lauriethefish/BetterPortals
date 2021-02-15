package com.lauriethefish.betterportals.shared.net.encryption;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import java.io.*;
import java.security.GeneralSecurityException;

public class EncryptedObjectStream implements IEncryptedObjectStream    {
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    private final CipherManager cipherManager;

    @Inject
    public EncryptedObjectStream(@Assisted InputStream inputStream, @Assisted OutputStream outputStream, CipherManager cipherManager) {
        this.inputStream = new DataInputStream(inputStream);
        this.outputStream = new DataOutputStream(outputStream);
        this.cipherManager = cipherManager;
    }

    @Override
    public Object readObject() throws GeneralSecurityException, IOException, ClassNotFoundException {
        int length = inputStream.readInt();

        if(length > MAX_REQUEST_SIZE) {
            throw new IllegalStateException(String.format("Requested length (%d bytes) was greater than the max request size of %d bytes", length, MAX_REQUEST_SIZE));
        }

        byte[] data = new byte[length];
        inputStream.readFully(data);

        byte[] decrypted = cipherManager.createDecrypt().doFinal(data);
        return new ObjectInputStream(new ByteArrayInputStream(decrypted)).readObject();
    }

    @Override
    public void writeObject(Object obj) throws GeneralSecurityException, IOException {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        new ObjectOutputStream(byteOutputStream).writeObject(obj);

        byte[] data = byteOutputStream.toByteArray();
        byte[] encrypted = cipherManager.createEncrypt().doFinal(data);

        if(encrypted.length > MAX_REQUEST_SIZE) {
            throw new IllegalStateException(String.format("Size of serialized and encrypted object (%d bytes) was greater than the maximum request size of %d bytes", encrypted.length, MAX_REQUEST_SIZE));
        }


        outputStream.writeInt(encrypted.length);
        outputStream.write(encrypted);
    }
}
