package org.service.registry;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NameTest {
    @Test
    void createNameRecord() {
        assertNotNull(Name.of("service-1"));
    }

    @Test
    void setNullToName() {
        assertThrows(IllegalArgumentException.class, () -> Name.of(null));
    }


}
