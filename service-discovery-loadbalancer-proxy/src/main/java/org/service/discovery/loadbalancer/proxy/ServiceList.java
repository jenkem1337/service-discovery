package org.service.discovery.loadbalancer.proxy;

import io.etcd.jetcd.KeyValue;

import java.util.List;

public record ServiceList(List<KeyValue> serviceList, long serviceCount) {
}
