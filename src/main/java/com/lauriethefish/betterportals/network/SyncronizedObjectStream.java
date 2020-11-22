package com.lauriethefish.betterportals.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class SyncronizedObjectStream {
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    private ReentrantLock outputLock = new ReentrantLock(true); // Use a fair lock to guarantee ordering of written objects
    private ReentrantLock inputLock = new ReentrantLock(true);

    private List<Object> skippedList = Collections.synchronizedList(new ArrayList<>());

    public SyncronizedObjectStream(ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    // Reads the next Object of type type from the stream, or fetches it from
    // skippedList if it was received previously
    public Object readNextOfType(Class<?> type) throws IOException, ClassNotFoundException {
        while (!inputLock.tryLock() || skippedList.size() > 0) {
            // Loop through any objects that were skipped because they weren't of the right type
            Iterator<Object> iterator = skippedList.iterator();
            while (iterator.hasNext()) {
                // Find the first of the correct type, then remove and return it
                Object obj = iterator.next();
                if (type.isInstance(obj)) {
                    iterator.remove();
                    return obj;
                }
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        // Read objects from the stream if one hasn't been read already
        while(true) {
            Object obj = inputStream.readObject();
            if(type.isInstance(obj)) { // If the object read had the right type, return it
                inputLock.unlock();
                return obj;
            }   else    {
                skippedList.add(obj); // Otherwise add it to the skipped list
            }
        }
    }
    
    // Write function that guarantees order using locks
    public void writeObject(Object obj) throws IOException  {
        outputLock.lock();
        outputStream.writeObject(obj);
        outputLock.unlock();
    }
}
