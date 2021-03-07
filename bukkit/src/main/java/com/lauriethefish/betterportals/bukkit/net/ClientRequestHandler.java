package com.lauriethefish.betterportals.bukkit.net;

import com.comphenix.protocol.wrappers.Pair;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.block.external.IExternalBlockWatcherManager;
import com.lauriethefish.betterportals.api.IntVector;
import com.lauriethefish.betterportals.bukkit.net.requests.CheckDestinationValidityRequest;
import com.lauriethefish.betterportals.bukkit.net.requests.GetBlockDataChangesRequest;
import com.lauriethefish.betterportals.bukkit.net.requests.GetSelectionRequest;
import com.lauriethefish.betterportals.bukkit.net.requests.TestForwardedRequest;
import com.lauriethefish.betterportals.bukkit.player.IPlayerDataManager;
import com.lauriethefish.betterportals.bukkit.player.selection.IPortalSelection;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.net.IRequestHandler;
import com.lauriethefish.betterportals.shared.net.RequestException;
import com.lauriethefish.betterportals.shared.net.Response;
import com.lauriethefish.betterportals.shared.net.requests.PreviousServerPutRequest;
import com.lauriethefish.betterportals.shared.net.requests.RelayRequest;
import com.lauriethefish.betterportals.shared.net.requests.Request;
import com.lauriethefish.betterportals.shared.net.requests.TeleportRequest;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

@Singleton
public class ClientRequestHandler implements IRequestHandler {
    private final Logger logger;
    private final IExternalBlockWatcherManager blockWatcherManager;
    private final IPlayerDataManager playerDataManager;
    private final IPortalClient portalClient;
    private final ConcurrentLinkedQueue<Pair<Request, Consumer<Response>>> awaitingHandling = new ConcurrentLinkedQueue<>();

    @Inject
    public ClientRequestHandler(Logger logger, IExternalBlockWatcherManager blockWatcherManager, IPlayerDataManager playerDataManager, IPortalClient portalClient) {
        this.logger = logger;
        this.blockWatcherManager = blockWatcherManager;
        this.playerDataManager = playerDataManager;
        this.portalClient = portalClient;
    }

    /**
     * Requests must be handled on the main thread, since they access the Bukkit API.
     * This is called by main update, and handles any pending requests
     */
    public void handlePendingRequests() {
        while(awaitingHandling.size() > 0) {
            Pair<Request, Consumer<Response>> next = awaitingHandling.remove();
            handleRequestInternal(next.getFirst(), next.getSecond());
        }
    }

    @Override
    public void handleRequest(@NotNull Request request, @NotNull Consumer<Response> onFinish) {
        awaitingHandling.add(new Pair<>(request, onFinish));
    }

    private void handleRequestInternal(@NotNull Request request, @NotNull Consumer<Response> onFinish) {
        logger.finer("Processing request of type: %s", request.getClass().getName());

        try {
            if(request instanceof RelayRequest) {
                handleRelayedRequest((RelayRequest) request, onFinish);
            }   else if(request instanceof GetBlockDataChangesRequest) {
                handleGetBlockDataChangesRequest((GetBlockDataChangesRequest) request, onFinish);
            }   else if(request instanceof TestForwardedRequest) {
                handleTestForwardedRequest((TestForwardedRequest) request, onFinish);
            }   else if(request instanceof CheckDestinationValidityRequest) {
                handleCheckDestinationValidityRequest((CheckDestinationValidityRequest) request, onFinish);
            }   else if(request instanceof TeleportRequest) {
                handleTeleportRequest((TeleportRequest) request, onFinish);
            }   else if(request instanceof GetSelectionRequest) {
                handleGetSelectionRequest((GetSelectionRequest) request, onFinish);
            }   else if(request instanceof PreviousServerPutRequest) {
                handlePreviousServerPutRequest((PreviousServerPutRequest) request, onFinish);
            }   else    {
                throw new IllegalStateException("Received request of unknown type");
            }
        }   catch(RequestException ex) {
            Response response = new Response();
            response.setError(ex);
            onFinish.accept(response);
        }   catch(Exception ex) {
            Response response = new Response();
            response.setError(new RequestException(ex, "Internal error occurred on a client server while processing request"));
            onFinish.accept(response);
        }
    }

    private void handleRelayedRequest(RelayRequest request, Consumer<Response> onFinish) throws IOException, ClassNotFoundException {
        Request innerRequest = request.getInnerRequest();
        handleRequest(innerRequest, (response) -> {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            try {
                new ObjectOutputStream(byteOutputStream).writeObject(response);
            } catch (IOException ex) {
                // This should never happen
            }

            // Wrap the response as a byte array to avoid the proxy trying to deserialize a type that doesn't exist on bungeecord
            Response wrappedResponse = new Response();
            wrappedResponse.setResult(byteOutputStream.toByteArray());
            onFinish.accept(wrappedResponse);
        });
    }

    private void handleGetBlockDataChangesRequest(@NotNull GetBlockDataChangesRequest request, @NotNull Consumer<Response> onFinish) {
        blockWatcherManager.onRequestReceived(request, onFinish);
    }

    private void handleTestForwardedRequest(@NotNull TestForwardedRequest request, @NotNull Consumer<Response> onFinish) throws RequestException  {
        logger.info("Received test forwarded request. Content: %s", request.getTestField());

        Response response = new Response();
        response.setResult(request.getTestField().add(new IntVector(0, 10, 0)));

        onFinish.accept(response);
    }

    private void handleCheckDestinationValidityRequest(@NotNull CheckDestinationValidityRequest request, @NotNull Consumer<Response> onFinish) throws RequestException {
        String gameVersion = VersionUtil.getCurrentVersion();
        if(!gameVersion.equals(request.getOriginGameVersion())) {
            throw new RequestException(String.format("Origin and destination servers are not on the same game version (%s on the destination vs %s on the origin)", gameVersion, request.getOriginGameVersion()));
        }

        if(Bukkit.getWorld(request.getDestinationWorldName()) == null && Bukkit.getWorld(request.getDestinationWorldId()) == null)  {
            throw new RequestException("Destination world no longer exists");
        }

        onFinish.accept(new Response());
    }

    private void handleTeleportRequest(@NotNull TeleportRequest request, @NotNull Consumer<Response> onFinish) {
        playerDataManager.setTeleportOnJoin(request);
        onFinish.accept(new Response());
    }

    private void handleGetSelectionRequest(@NotNull GetSelectionRequest request, @NotNull Consumer<Response> onFinish) throws RequestException  {
        Response response = new Response();

        IPortalSelection destSelection = playerDataManager.getDestinationSelectionWhenLoggedOut(request.getPlayerId());
        response.setResult(destSelection != null && destSelection.isValid() ? new GetSelectionRequest.ExternalSelectionInfo(destSelection) : null);

        logger.fine("Returning selection %s", destSelection);
        onFinish.accept(response);
    }

    private void handlePreviousServerPutRequest(@NotNull PreviousServerPutRequest request, @NotNull Consumer<Response> onFinish) throws RequestException {
        String previousServer = request.getPreviousServer();

        GetSelectionRequest getSelectionRequest = new GetSelectionRequest();
        getSelectionRequest.setPlayerId(request.getPlayerId());
        logger.fine("Previous server: %s", previousServer);

        portalClient.sendRequestToServer(getSelectionRequest, previousServer, (response) -> {
            try {
                GetSelectionRequest.ExternalSelectionInfo selectionInfo = (GetSelectionRequest.ExternalSelectionInfo) response.getResult();
                if(selectionInfo != null) {
                    selectionInfo.getPosition().setServerName(previousServer);
                }
                logger.fine("Selection info: %s", selectionInfo);
                playerDataManager.setExternalSelectionOnLogin(request.getPlayerId(), selectionInfo);
                onFinish.accept(new Response());
            }   catch(RequestException ex) {
                logger.warning("An error occurred while trying to sync destination selection for player with ID %s", request.getPlayerId());
                ex.printStackTrace();
            }
        });
    }
}
