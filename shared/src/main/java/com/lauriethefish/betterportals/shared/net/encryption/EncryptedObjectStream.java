package com.lauriethefish.betterportals.shared.net.encryption;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

        GZIPInputStream decompressionStream = new GZIPInputStream(new ByteArrayInputStream(data));
        CipherInputStream decryptionStream = new CipherInputStream(decompressionStream, cipherManager.createDecrypt());

        return new ObjectInputStream(decryptionStream).readObject();
    }

    @Override
    public void writeObject(Object obj) throws GeneralSecurityException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream compressionStream = new GZIPOutputStream(byteArrayOutputStream);
        CipherOutputStream encryptionStream = new CipherOutputStream(compressionStream, cipherManager.createEncrypt());

        new ObjectOutputStream(encryptionStream).writeObject(obj);
        encryptionStream.close();
        compressionStream.close();

        byte[] data = byteArrayOutputStream.toByteArray();

        if(data.length > MAX_REQUEST_SIZE) {
            throw new IllegalStateException(String.format("Size of serialized and encrypted object (%d bytes) was greater than the maximum request size of %d bytes", data.length, MAX_REQUEST_SIZE));
        }

        outputStream.writeInt(data.length);
        outputStream.write(data);
    }
}
