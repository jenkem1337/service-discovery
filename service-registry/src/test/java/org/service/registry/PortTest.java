package org.service.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PortTest {
    @Test
    void createPortValueObject() {
        assertNotNull(Port.of(443));
    }

    @Test
    void whenPortNumberNegative_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Port.of(-123));
    }

    @Test
    void whenPortNumberBiggerThan65535_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Port.of(65536));

    }
}
