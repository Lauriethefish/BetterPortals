import com.lauriethefish.betterportals.shared.net.encryption.CipherManager;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.AEADBadTagException;

import static org.junit.jupiter.api.Assertions.*;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

public class EncryptionTests {
    private CipherManager cipherManager;
    private byte[] data;

    @Before
    public void setUp() throws NoSuchAlgorithmException {
        cipherManager = new CipherManager();
        cipherManager.init(UUID.fromString("c27769c6-41f1-4bd7-a0f1-b6fb62b213a7"));

        Random random = new Random(0);
        data = new byte[1024];
        random.nextBytes(data);
    }

    @Test
    public void testEncryptAndDecrypt() throws GeneralSecurityException {
        byte[] encrypted = cipherManager.createEncrypt().doFinal(data);
        byte[] decrypted = cipherManager.createDecrypt().doFinal(encrypted);

        for(int i = 0; i < data.length; i++) {
            assertEquals(data[i], decrypted[i]);
        }
    }

    @Test(expected = AEADBadTagException.class)
    public void testInvalidKey() throws GeneralSecurityException {
        CipherManager invalidCipher = new CipherManager();
        invalidCipher.init(UUID.fromString("6f653d86-c69c-4d3c-95b3-38037eedddd7"));

        byte[] encrypted = cipherManager.createEncrypt().doFinal(data);
        invalidCipher.createDecrypt().doFinal(encrypted);
    }
}
