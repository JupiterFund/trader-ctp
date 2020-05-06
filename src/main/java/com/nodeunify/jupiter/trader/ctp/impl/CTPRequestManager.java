package com.nodeunify.jupiter.trader.ctp.impl;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Strings;
import com.nodeunify.jupiter.trader.ctp.util.CTPUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Component
public class CTPRequestManager {

    @Value("${app.trader.id:0}")
    private int traderID;

    private int frontID = 0;
    private int sessionID = 0;
    private String orderRefPrefix;
    // TODO: check orderActionRef & orderRef
    // TODO: check int or long for both counters
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicInteger orderRequestCount = new AtomicInteger();
    private final Map<Integer, CompletableFuture<?>> requestListeners = new ConcurrentHashMap<>();
    private final Map<Integer, String> reqUUIDRegistry = new ConcurrentHashMap<>();
    private final Map<Integer, String> reqOrderRefRegistry = new ConcurrentHashMap<>();

    public int getRequestID() {
        return requestCount.get();
    }

    public int getAndIncrementRequestID() {
        if (requestCount.get() >= CTPUtil.MAX_INT_VALUE) {
            requestCount.set(0);
        }
        return requestCount.getAndIncrement();
    }

    public void configOrderRefPrefix() {
        if (traderID == 0) {
            long seed = Long.parseLong("" + frontID + Math.abs(sessionID), 10);
            Random random = new Random(seed);
            orderRefPrefix = String.valueOf(random.nextInt(1000000));
        } else {
            orderRefPrefix = String.valueOf(traderID);
        }
        log.debug("[configOrderRefPrefix] 设置OrderRef前缀. orderRefPrefix:{}", orderRefPrefix);
    }

    public String getOrderRef() {
        return getOrderRefPrefix() + String.valueOf(orderRequestCount.get());
    }

    public String getAndIncrementOrderRef() {
        return getOrderRefPrefix() + String.valueOf(orderRequestCount.getAndIncrement());
    }

    public void registerUUID(int idOrRef, String uuid) {
        log.debug("[registerUUID] 注册请求UUID. idOrRef:{}; uuid:{}", idOrRef, uuid);
        if (!Strings.isNullOrEmpty(uuid)) {
            this.reqUUIDRegistry.put(idOrRef, uuid);
        }
    }

    public void registerOrderRef(int requestID, String orderRef) {
        log.debug("[registerOrderRef] 注册报单引用. requestID:{}; orderRef:{}", requestID, orderRef);
        if (!Strings.isNullOrEmpty(orderRef)) {
            this.reqOrderRefRegistry.put(requestID, orderRef);
        }
    }

    public String lookupUUID(int idOrRef) {
        return this.reqUUIDRegistry.get(idOrRef);
    }

    public void addListener(int requestID, CompletableFuture<?> listener) {
        log.debug("[addListener] 添加回调监听. requestID:{}", requestID);
        this.requestListeners.put(requestID, listener);
    }

    public void addOrderRefListener(int requestID, CompletableFuture<?> listener) {
        log.debug("[addOrderRefListener] 添加报单回调监听. requestID:{}", requestID);
        String orderRef = this.reqOrderRefRegistry.get(requestID);
        this.requestListeners.put(Integer.parseInt(orderRef), listener);
    }

    public CompletableFuture<?> getListener(int requestID) {
        return this.requestListeners.get(requestID);
    }

    public CompletableFuture<?> getOrderRefListener(String orderRef) {
        return this.requestListeners.get(Integer.parseInt(orderRef));
    }

}
