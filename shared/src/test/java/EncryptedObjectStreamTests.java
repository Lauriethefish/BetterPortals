import com.lauriethefish.betterportals.shared.net.encryption.CipherManager;
import com.lauriethefish.betterportals.shared.net.encryption.EncryptedObjectStream;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncryptedObjectStreamTests {
    private static class TestTransmit implements Serializable {
        int testFieldA;
        String testFieldB;

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof TestTransmit)) {
                return false;
            }
            TestTransmit otherInstance = (TestTransmit) other;

            return otherInstance.testFieldA == testFieldA && otherInstance.testFieldB.equals(testFieldB);
        }
    }

    private CipherManager cipherManager;

    @Before
    public void setUp() throws NoSuchAlgorithmException {
        this.cipherManager = new CipherManager();
        cipherManager.init(UUID.randomUUID());
    }

    @Test
    public void testSendAndReceiveObject() throws GeneralSecurityException, IOException, ClassNotFoundException {
        ByteArrayOutputStream testOutput = new ByteArrayOutputStream();

        EncryptedObjectStream testStream = new EncryptedObjectStream(null, testOutput, cipherManager);

        TestTransmit obj = new TestTransmit();
        obj.testFieldA = 42;
        obj.testFieldB = "Fish man";

        testStream.writeObject(obj);

        byte[] data = testOutput.toByteArray();

        ByteArrayInputStream testInput = new ByteArrayInputStream(data);

        testStream = new EncryptedObjectStream(testInput, null, cipherManager);
        TestTransmit result = (TestTransmit) testStream.readObject();

        assertEquals(obj, result);
    }

}
