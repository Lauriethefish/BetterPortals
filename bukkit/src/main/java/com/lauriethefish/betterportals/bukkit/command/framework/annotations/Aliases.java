package com.lauriethefish.betterportals.bukkit.command.framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Specifies alternative names for this command
// These names shouldn't be fully qualified. e.g. for betterportals/doThing, an alias can just be thing
// None of these names are case-sensitive
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Aliases {
    String[] value();
}
