package com.nodeunify.jupiter.trader.ctp.config;

import com.nodeunify.jupiter.trader.ctp.impl.CTPTraderApi;
import com.nodeunify.jupiter.trader.ctp.impl.CTPTraderSpi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CTPTraderConfig {

    @Bean
    public CTPTraderApi traderApi() {
        return CTPTraderApi.createFtdcTraderApi("trade");
    }

    @Bean
    public CTPTraderSpi traderSpi() {
        return new CTPTraderSpi();
    }
    
}
