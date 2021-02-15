package com.lauriethefish.betterportals.bukkit.net.requests;

import com.lauriethefish.betterportals.bukkit.math.IntVector;
import com.lauriethefish.betterportals.shared.net.requests.Request;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestForwardedRequest extends Request {
    private IntVector testField;
}
