package com.lauriethefish.betterportals.bukkit.command.framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Specifies the subcommands that lead up to this command (if any), as well as the default command name
// No parts of the path are case sensitive
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Path {
    String value();
}
