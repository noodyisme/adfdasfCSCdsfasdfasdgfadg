package com.capitalone.identity.identitybuilder.model.abac;

import org.springframework.lang.NonNull;

import java.util.Objects;

public class Client {

    @NonNull
    final String id;

    @NonNull
    final AccessGrant effect;

    public Client(String id, AccessGrant effect) {
        this.id = Objects.requireNonNull(id);
        this.effect = Objects.requireNonNull(effect);
    }
}
