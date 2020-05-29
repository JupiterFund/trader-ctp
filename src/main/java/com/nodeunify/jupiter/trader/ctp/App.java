package com.nodeunify.jupiter.trader.ctp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class App {
    
    @Autowired
    CTPTrader ctpTrader;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    /**
     * 由于启动CTPTrader必须挂起主进程，因此使用事件回调，以确保CTPTrader在GRPC服务
     * 接口之后开启.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startCTPTrader() {
        log.info("启动CTPTrader");
        ctpTrader.start();
    }
}
