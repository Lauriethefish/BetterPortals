package com.lauriethefish.betterportals.shared.net.encryption;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Individually serializes objects to avoid issues with block size.
 * Specifically, directly using a {@link javax.crypto.CipherOutputStream} isn't really possible since it has no way to flush pending bytes.
 */
public interface IEncryptedObjectStream {
    /**
     * Used to prevent a bad actor setting the request size to a very large number and filling up our memory.
     */
    int MAX_REQUEST_SIZE = 31_457_280;

    /**
     * Reads the 4 byte length prefix, then the next object from the stream
     * @return The next read object
     * @throws GeneralSecurityException If an error occurred during decryption
     * @throws IOException Any IO related exception in the underlying stream
     * @throws ClassNotFoundException If the object stream encounters an object that is not loaded on the JVM.
     * @throws IllegalStateException If the requested read length is greater than {@link IEncryptedObjectStream#MAX_REQUEST_SIZE}
     */
    Object readObject() throws GeneralSecurityException, IOException, ClassNotFoundException;

    /**
     * Writes the 4 byte length prefix, then <code>obj</code> to the underlying output stream.
     * @param obj The object to write
     * @throws GeneralSecurityException Any encryption errors
     * @throws IOException Any IO related exception in the underlying stream.
     */
    void writeObject(Object obj) throws GeneralSecurityException, IOException;
}
