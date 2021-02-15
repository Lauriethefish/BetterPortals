package implementations;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.block.multiblockchange.IMultiBlockChangeManager;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class TestMultiBlockChangeManager implements IMultiBlockChangeManager {
    @Getter private final Map<Vector, WrappedBlockData> changes = new HashMap<>();
    @Getter private final Player player;
    @Getter private boolean wereChangesSent = false;

    @Inject
    public TestMultiBlockChangeManager(@Assisted Player player) {
        this.player = player;
    }

    @Override
    public void addChange(Vector position, WrappedBlockData newData) {
        changes.put(position, newData);
    }

    @Override
    public void sendChanges() {
        wereChangesSent = true;
    }
}
