package org.service.registry;

import java.util.function.Consumer;

public record WatchRequest(String key, Consumer watchCallback) {
}
