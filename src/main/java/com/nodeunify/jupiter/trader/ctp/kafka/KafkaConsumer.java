package com.nodeunify.jupiter.trader.ctp.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.nodeunify.jupiter.commons.mapper.TraderCTPMapper;
import com.nodeunify.jupiter.trader.ctp.impl.CTPTraderApi;
import com.nodeunify.jupiter.trader.ctp.impl.CTPRequestManager;
import com.nodeunify.jupiter.trader.ctp.v1.Order;
import com.nodeunify.jupiter.trader.ctp.v1.OrderAction;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import ctp.thosttraderapi.CThostFtdcInputOrderActionField;
import ctp.thosttraderapi.CThostFtdcInputOrderField;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaConsumer {

    @Autowired
    private CTPTraderApi traderApi;
    @Autowired
    private CTPRequestManager ctpRequestManager;

    @KafkaListener(id = "orderInsertListner", topics = "${spring.kafka.topic.trader.ctp.order.insert}", autoStartup = "false")
    public void listenOrderInsert(ConsumerRecord<String, byte[]> record) {
        try {
            Order order = Order.parseFrom(record.value());
            String uuid = order.getUUID();
            int requestID = ctpRequestManager.getAndIncrementRequestID();
            ctpRequestManager.registerUUID(requestID, uuid);
            String orderRef = ctpRequestManager.getAndIncrementOrderRef();
            log.debug("orderRef: {}", orderRef);
            ctpRequestManager.registerUUID(Integer.parseInt(orderRef), uuid);
            log.debug("registerOrderRef: {}, {}", requestID, orderRef);
            ctpRequestManager.registerOrderRef(requestID, orderRef);
            CThostFtdcInputOrderField inputOrderField = TraderCTPMapper.MAPPER.map(order);
            inputOrderField.setOrderRef(orderRef);
            traderApi.reqOrderInsert(inputOrderField, requestID);
        } catch (InvalidProtocolBufferException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @KafkaListener(id = "orderActionListener", topics = "${spring.kafka.topic.trader.ctp.order.action}", autoStartup = "false")
    public void listenOrderAction(ConsumerRecord<String, byte[]> record) {
        try {
            OrderAction orderAction = OrderAction.parseFrom(record.value());
            String uuid = orderAction.getUUID();
            int requestID = ctpRequestManager.getAndIncrementRequestID();
            ctpRequestManager.registerUUID(requestID, uuid);
            String orderActionRef = ctpRequestManager.getAndIncrementOrderRef();
            ctpRequestManager.registerUUID(Integer.parseInt(orderActionRef), uuid);
            ctpRequestManager.registerOrderRef(requestID, orderActionRef);
            CThostFtdcInputOrderActionField inputOrderActionField = TraderCTPMapper.MAPPER.map(orderAction);
            inputOrderActionField.setOrderActionRef(Integer.parseInt(orderActionRef));
            inputOrderActionField.setFrontID(ctpRequestManager.getFrontID());
            inputOrderActionField.setSessionID(ctpRequestManager.getSessionID());
            traderApi.reqOrderAction(inputOrderActionField, requestID);
        } catch (InvalidProtocolBufferException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
}
