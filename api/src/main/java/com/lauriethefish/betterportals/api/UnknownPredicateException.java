package com.lauriethefish.betterportals.api;

import org.jetbrains.annotations.NotNull;

public class UnknownPredicateException extends IllegalArgumentException {
    public UnknownPredicateException(@NotNull PortalPredicate predicate) {
        super("Attempted to remove predicate that wasn't added. Type: " + predicate.getClass().getName());
    }
}
