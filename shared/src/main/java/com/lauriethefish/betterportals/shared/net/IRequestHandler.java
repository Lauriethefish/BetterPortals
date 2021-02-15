package com.lauriethefish.betterportals.shared.net;

import com.lauriethefish.betterportals.shared.net.requests.Request;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Represents a class that handles requests of some sort.
 */
public interface IRequestHandler {
    /**
     * Processes <code>request</code> and gives the response to <code>onFinish</code>.
     * Note: This function will likely return before processing is actually finished, since it may result in the request being sent to other servers first.
     * @param request The request to process
     * @param onFinish Given the response once processing is finished.
     */
    void handleRequest(@NotNull Request request, @NotNull Consumer<Response> onFinish);
}
