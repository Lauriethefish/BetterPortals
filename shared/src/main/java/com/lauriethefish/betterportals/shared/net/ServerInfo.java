package com.lauriethefish.betterportals.shared.net;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ServerInfo implements Serializable {
    private static final long serialVersionUID = 1;

    private String name;
    private String gameVersion;

    @Override
    public String toString() {
        return String.format("(Server name: %s. Server version: %s)", name, gameVersion);
    }
}
