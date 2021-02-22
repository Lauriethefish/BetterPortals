package com.lauriethefish.betterportals.shared.net.requests;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public abstract class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Request IDs are used to preserve order when sending and receiving many requests
     */
    @Getter @Setter private int id;
}
