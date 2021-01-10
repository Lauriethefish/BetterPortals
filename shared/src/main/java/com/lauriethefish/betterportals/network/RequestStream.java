package com.lauriethefish.betterportals.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.lauriethefish.betterportals.network.encryption.EncryptionManager;

//import static java.lang.System.out;

// Used to read and write requests/responses and guarantees ordering of sent and received objects
// Also uses encryption for security n'stuff
public class RequestStream {
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private ReentrantLock outputLock = new ReentrantLock(true); // Use a fair lock to guarantee ordering of written objects
    private ReentrantLock inputLock = new ReentrantLock(true);

    private EncryptionManager encryptionManager; // Handles encrypting/decrypting requests

    private ReentrantLock skippedListLock = new ReentrantLock(true);
    private List<Object> skippedList = new ArrayList<>();


    public RequestStream(InputStream inputStream, OutputStream outputStream, EncryptionManager encryptionManager) throws GeneralSecurityException {
        this.inputStream = new DataInputStream(inputStream);
        this.outputStream = new DataOutputStream(outputStream);
        this.encryptionManager = encryptionManager;
    }

    // Reads the next Object of type type from the stream, or fetches it from
    // skippedList if it was received previously
    public Object readNextOfType(Class<?> type) throws IOException, ClassNotFoundException, GeneralSecurityException {
        //out.println("Reading next object of type " + type.getName());
        //out.println(skippedList.size());

        while(true) {
            boolean breakOnFinish = inputLock.tryLock();

            skippedListLock.lock();
            // Loop through any objects that were skipped because they weren't of the right type
            Iterator<Object> iterator = skippedList.iterator();
            while (iterator.hasNext()) {
                // Find the first of the correct type, then remove and return it
                Object obj = iterator.next();
                if (type.isInstance(obj)) {
                    if(inputLock.isHeldByCurrentThread()) {inputLock.unlock();}

                    iterator.remove();
                    skippedListLock.unlock();
                    //out.println("Found in skipped objects");
                    return obj;
                }
            }
            skippedListLock.unlock();

            if(breakOnFinish) {break;}

            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        //out.println("Reading objects - none skipped found!");
        // Read objects from the stream if one hasn't been read already
        while(true) {
            Object obj = readObject();
            if(type.isInstance(obj)) { // If the object read had the right type, return it
                inputLock.unlock();
                return obj;
            }   else    {
                skippedListLock.lock();
                skippedList.add(obj); // Otherwise add it to the skipped list
                skippedListLock.unlock();
            }
        }
    }

    private Object readObject() throws IOException, ClassNotFoundException, GeneralSecurityException {
        byte[] data = new byte[inputStream.readInt()]; // Make a new byte array for the data, reading the length from the stream
        inputStream.readFully(data);

        byte[] decrypted = encryptionManager.decrypt(data); // Decrypt the data
        return SerializationUtils.deserialize(decrypted); // Deserialize it into an object
    }
    
    // Write function that guarantees order using locks
    public void writeObject(Object obj) throws IOException, GeneralSecurityException  {
        //out.println("Sending object of type " + obj.getClass());

        byte[] data = SerializationUtils.serialize(obj);
        outputLock.lock();
        byte[] encryptedData = encryptionManager.encrypt(data); // Make sure this is inside the locked block
        // First write the length of the encrypted request, then the request itself
        outputStream.writeInt(encryptedData.length);
        outputStream.write(encryptedData);

        outputLock.unlock();
    }
}
