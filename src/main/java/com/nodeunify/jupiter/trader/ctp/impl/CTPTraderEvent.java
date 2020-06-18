package com.nodeunify.jupiter.trader.ctp.impl;

import org.springframework.context.ApplicationEvent;

public class CTPTraderEvent extends ApplicationEvent {

    /**
     *
     */
    private static final long serialVersionUID = -169292482194802658L;

    public CTPTraderEvent(Object source) {
        super(source);
    }
    
}
