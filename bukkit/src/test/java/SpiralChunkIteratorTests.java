import com.lauriethefish.betterportals.bukkit.chunk.chunkpos.ChunkPosition;
import com.lauriethefish.betterportals.bukkit.chunk.chunkpos.SpiralChunkAreaIterator;
import org.junit.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpiralChunkIteratorTests {
    // Check that the spiral iterator actually goes around in a spiral
    @Test
    public void testPath() {
        SpiralChunkAreaIterator iterator = new SpiralChunkAreaIterator(new ChunkPosition(0, 0), new ChunkPosition(2, 2));
        ChunkPosition[] expectedPath = new ChunkPosition[] {
                new ChunkPosition(1, 1),
                new ChunkPosition(2, 1),
                new ChunkPosition(2, 2),
                new ChunkPosition(1, 2),
                new ChunkPosition(0, 2),
                new ChunkPosition(0, 1),
                new ChunkPosition(0, 0),
                new ChunkPosition(1, 0),
                new ChunkPosition(2, 0),
        };

        // Check that the iterator follows the expected spiral path
        int i = 0;
        while(iterator.hasNext()) {
            ChunkPosition pos = iterator.next();
            assertEquals(expectedPath[i], pos);
            i++;
        }

        assertEquals(expectedPath.length, i);
    }

    // Test that the spiral iterator always finishes with a bunch of random positions
    @Test
    public void testFinishes() {
        Random random = new Random();

        int bound = 50;
        for(int run = 0; run < 100; run++) { // Run 100 random tests
            // Create an iterator with random coordinates
            ChunkPosition low = new ChunkPosition(random.nextInt(bound), random.nextInt(bound));
            ChunkPosition high = new ChunkPosition(random.nextInt(bound), random.nextInt(bound));
            SpiralChunkAreaIterator iterator = new SpiralChunkAreaIterator(low, high);

            boolean reachedEnd = false;
            for(int i = 0; i < bound * bound; i++) {
                if(!iterator.hasNext()) {
                    reachedEnd = true;
                    break;
                }

                iterator.next();
            }

            assertTrue(reachedEnd); // Make sure it successfully exited
        }

    }
}