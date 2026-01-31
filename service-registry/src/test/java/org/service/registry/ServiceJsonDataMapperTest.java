package org.service.registry;

import com.dslplatform.json.DslJson;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceJsonDataMapperTest {
    @Test
    void objectToString() throws IOException {
        DslJson<Object> dslJson = new DslJson<>();
        var serviceJsonDataMapper = new ServiceJsonDataMapper(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "user-service", "123.112.122.111", 9090,"http", Instant.now().toString());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        dslJson.serialize(serviceJsonDataMapper, os);
        var osByteArray = os.toByteArray();
        ServiceJsonDataMapper copy =
                dslJson.deserialize(ServiceJsonDataMapper.class, osByteArray, osByteArray.length);

        assertEquals("user-service", copy.serviceName);
        assertEquals(9090, copy.port);
    }

}