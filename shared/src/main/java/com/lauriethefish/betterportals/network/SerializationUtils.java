package com.lauriethefish.betterportals.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// Some convenience functions for reading from/writing objects to byte arrays
// These byte arrays are then processed by the cipher for encryption
public class SerializationUtils {
    public static byte[] serialize(Object obj) {
        // Make a ByteArrayOutputStream to serialize the object to a byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
        }   catch(IOException ex) {
            ex.printStackTrace(); // This should never happen, since we're writing to a byte array
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static Object deserialize(byte[] buffer) throws ClassNotFoundException   {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return objectInputStream.readObject();
        }   catch(IOException ex) {
            ex.printStackTrace(); // This should never happen, since we're reading from a byte array
            return null;
        }
    }
}
