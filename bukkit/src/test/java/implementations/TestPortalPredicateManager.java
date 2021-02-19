package implementations;

import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.predicate.IPortalPredicateManager;
import com.lauriethefish.betterportals.bukkit.portal.predicate.PortalPredicate;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class TestPortalPredicateManager implements IPortalPredicateManager {
    @Getter private final Set<IPortal> activatablePortals = new HashSet<>();
    @Getter private final Set<IPortal> viewablePortals = new HashSet<>();
    @Getter private final Set<IPortal> teleportablePortals = new HashSet<>();

    @Override
    public void addActivationPredicate(PortalPredicate predicate) { }

    @Override
    public void addViewPredicate(PortalPredicate predicate) { }

    @Override
    public void addTeleportPredicate(PortalPredicate predicate) {

    }

    @Override
    public boolean isActivatable(IPortal portal, Player player) {
        return activatablePortals.contains(portal);
    }

    @Override
    public boolean isViewable(IPortal portal, Player player) {
        return viewablePortals.contains(portal);
    }

    @Override
    public boolean canTeleport(IPortal portal, Player player) {
        return teleportablePortals.contains(portal);
    }

    public void addActivatablePortal(IPortal portal) {
        activatablePortals.add(portal);
    }

    public void addViewablePortal(IPortal portal) {
        viewablePortals.add(portal);
    }
}
