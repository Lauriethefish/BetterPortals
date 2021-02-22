package com.lauriethefish.betterportals.shared.net.encryption;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public interface EncryptedObjectStreamFactory {
    IEncryptedObjectStream create(InputStream inputStream, OutputStream outputStream) throws GeneralSecurityException;
}
