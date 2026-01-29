package org.service.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IpAddressTest {

    @Test
    void createIpAddressRecord(){
        assertNotNull(IpAddress.of("123.123.123.123"));
    }
    @Test
    void setIncorrectIpAddress(){
        assertThrows(IllegalArgumentException.class , () -> {
            IpAddress.of("1233.222.222.222");
        });
    }
}
