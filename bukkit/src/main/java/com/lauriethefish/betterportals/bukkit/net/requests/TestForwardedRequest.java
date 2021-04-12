package com.lauriethefish.betterportals.bukkit.net.requests;

import com.lauriethefish.betterportals.api.IntVector;
import com.lauriethefish.betterportals.shared.net.requests.Request;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestForwardedRequest extends Request {
    private static final long serialVersionUID = 1L;

    private IntVector testField;
}
