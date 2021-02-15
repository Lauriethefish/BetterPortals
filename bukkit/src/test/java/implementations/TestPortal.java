package implementations;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.block.IViewableBlockArray;
import com.lauriethefish.betterportals.bukkit.entity.IPortalEntityList;
import com.lauriethefish.betterportals.bukkit.math.PortalTransformations;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import lombok.Getter;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

import java.util.UUID;

public class TestPortal implements IPortal {
    @Getter private final PortalPosition originPos;
    @Getter private final PortalPosition destPos;
    @Getter private final UUID id;
    @Getter private final Vector size;

    @Getter private int updateCallCount = 0;
    @Getter private int activateCallCount = 0;
    @Getter private int deactivateCallCount = 0;
    @Getter private int removeCallCount = 0;
    @Getter private int viewActivateCallCount;
    @Getter private int viewDeactivateCallCount;
    @Getter private int viewUpdateCallCount;

    @Inject
    public TestPortal(@Assisted("originPos") @NotNull PortalPosition originPos,
                      @Assisted("destPos") @NotNull PortalPosition destPos,
                      @NotNull Vector size, @Assisted boolean isCustom,
                      @NotNull @Assisted("id") UUID id,
                      @Nullable @Assisted("ownerId") UUID ownerId,
                      @Nullable @Assisted("name") String name) {
        this.originPos = originPos;
        this.destPos = destPos;
        this.id = id;
        this.size = size;
    }

    @Override
    public @Nullable UUID getOwnerId() {
        return null;
    }

    @Override
    public @Nullable String getName() {
        return null;
    }

    @Override
    public void setName(@Nullable String name) {

    }

    @Override
    public void onUpdate() {
        updateCallCount += 1;
    }

    @Override
    public void onViewUpdate() {
        viewUpdateCallCount += 1;
    }

    @Override
    public void onActivate() {
        activateCallCount += 1;
    }

    @Override
    public void onDeactivate() {
        deactivateCallCount += 1;
    }

    @Override
    public void onViewActivate() {
        viewActivateCallCount += 1;
    }

    @Override
    public void onViewDeactivate() {
        viewDeactivateCallCount += 1;
    }

    @Override
    public boolean isCrossServer() {
        return false;
    }

    @Override
    public boolean isCustom() {
        return false;
    }

    @Override
    public @NotNull PortalTransformations getTransformations() {
        return null;
    }

    @Override
    public @NotNull IViewableBlockArray getViewableBlocks() {
        return null;
    }

    @Override
    public @NotNull IPortalEntityList getEntityList() {
        return null;
    }

    @Override
    public void remove(boolean removeOtherDirection) {
        removeCallCount += 1;
    }

    @Override
    public boolean isRegistered() {
        return false;
    }
}
