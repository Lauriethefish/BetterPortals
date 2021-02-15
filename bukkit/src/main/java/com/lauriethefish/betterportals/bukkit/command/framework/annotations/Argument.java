package com.lauriethefish.betterportals.bukkit.command.framework.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Arguments.class)
public @interface Argument   {
    String name();
    String defaultValue() default "";
}
