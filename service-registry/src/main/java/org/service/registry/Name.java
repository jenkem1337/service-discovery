package org.service.registry;

import java.util.Objects;

public record Name(String name) {

    public static Name of(String name) {
        if(Objects.isNull(name)){
            throw new IllegalArgumentException("Name is null");
        }
        return new Name(name.strip());
    }
}
