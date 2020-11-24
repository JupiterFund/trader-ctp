package com.nodeunify.jupiter.trader.ctp.kafka;

import com.google.common.base.Strings;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.nodeunify.jupiter.commons.mapper.TraderCTPMapper;
import com.nodeunify.jupiter.trader.ctp.v1.Error;
import com.nodeunify.jupiter.trader.ctp.v1.Instrument;
import com.nodeunify.jupiter.trader.ctp.v1.InvestorPosition;
import com.nodeunify.jupiter.trader.ctp.v1.Order;
import com.nodeunify.jupiter.trader.ctp.v1.Trade;
import com.nodeunify.jupiter.trader.ctp.v1.TradingAccount;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import ctp.thosttraderapi.CThostFtdcInputOrderActionField;
import ctp.thosttraderapi.CThostFtdcInputOrderField;
import ctp.thosttraderapi.CThostFtdcInstrumentField;
import ctp.thosttraderapi.CThostFtdcInvestorPositionField;
import ctp.thosttraderapi.CThostFtdcOrderActionField;
import ctp.thosttraderapi.CThostFtdcOrderField;
import ctp.thosttraderapi.CThostFtdcRspInfoField;
import ctp.thosttraderapi.CThostFtdcTradeField;
import ctp.thosttraderapi.CThostFtdcTradingAccountField;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaProducer {
    
    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${spring.kafka.topic.trader.ctp.rsp.instrument}")
    private String instrumentTopic;
    @Value("${spring.kafka.topic.trader.ctp.rsp.investor-position}")
    private String investorPositionTopic;
    @Value("${spring.kafka.topic.trader.ctp.rsp.trading-account}")
    private String tradingAccountTopic;
    @Value("${spring.kafka.topic.trader.ctp.rsp.order}")
    private String orderTopic;
    @Value("${spring.kafka.topic.trader.ctp.rsp.trade}")
    private String tradeTopic;
    @Value("${spring.kafka.topic.trader.ctp.rtn.order}")
    private String rtnOrderTopic;
    @Value("${spring.kafka.topic.trader.ctp.rtn.trade}")
    private String rtnTradeTopic;
    @Value("${spring.kafka.topic.trader.ctp.err.order.insert}")
    private String errOrderInsertTopic;
    @Value("${spring.kafka.topic.trader.ctp.err.order.action}")
    private String errOrderActionTopic;

    private Message compactUUID(GeneratedMessageV3 message, String uuid) {
        Message.Builder builder = message.toBuilder();
        Descriptor descriptor  = builder.getDescriptorForType();
        FieldDescriptor fd = descriptor.findFieldByName("UUID");
        if (!Strings.isNullOrEmpty(uuid)) {
            builder.setField(fd, uuid);
        }
        return builder.build();
    }

    private void send(String topic, Message message) {
        kafkaTemplate.send(new ProducerRecord<String, byte[]>(topic, message.toByteArray()));
    }

    private void send(String topic, String uuid, GeneratedMessageV3 message) {
        Message compacted = compactUUID(message, uuid);
        send(topic, compacted);
        log.debug("message sent: {}", topic);
    }

    public void send(String uuid, CThostFtdcInstrumentField instrumentField) {
        log.debug("[send] Query instrument: {}", instrumentField.getInstrumentID());
        Instrument instrument = TraderCTPMapper.MAPPER.map(instrumentField);
        send(instrumentTopic, uuid, instrument);
    }

    public void send(String uuid, CThostFtdcInvestorPositionField investorPositionField) {
        log.debug("[send] Query investorPosition: {}", investorPositionField.getInstrumentID());
        InvestorPosition investorPosition = TraderCTPMapper.MAPPER.map(investorPositionField);
        send(investorPositionTopic, uuid, investorPosition);
    }

    public void send(String uuid, CThostFtdcTradingAccountField tradingAccountField) {
        log.debug("[send] Query tradingAccount: {}", tradingAccountField.getAccountID());
        TradingAccount tradingAccount = TraderCTPMapper.MAPPER.map(tradingAccountField);
        send(tradingAccountTopic, uuid, tradingAccount);
    }

    // trader.ctp.rsp.order
    public void send(String uuid, CThostFtdcOrderField orderField) {
        log.debug("[send] Query order: {}", orderField.getOrderRef());
        Order order = TraderCTPMapper.MAPPER.map(orderField);
        send(orderTopic, uuid, order);
    }

    // trader.ctp.rsp.trade
    public void send(String uuid, CThostFtdcTradeField tradeField) {
        log.debug("[send] Query trade: {}", tradeField.getTradeID());
        Trade trade = TraderCTPMapper.MAPPER.map(tradeField);
        send(tradeTopic, uuid, trade);
    }

    // trader.ctp.rtn.order
    public void sendReturnOrder(String uuid, CThostFtdcOrderField orderField) {
        log.debug("[send] Return order: {}", orderField.getOrderRef());
        Order order = TraderCTPMapper.MAPPER.map(orderField);
        send(rtnOrderTopic, uuid, order);
    }

    // trader.ctp.rtn.trade
    public void sendReturnTrade(String uuid, CThostFtdcTradeField tradeField) {
        // TODO: 临时扩展日志，用于调错
        log.debug("[send] Return trade: {}, {}", rtnTradeTopic, tradeField.getTradeID());
        Trade trade = TraderCTPMapper.MAPPER.map(tradeField);
        send(rtnTradeTopic, uuid, trade);
        log.debug("[send] Kafka written: {}, {}", rtnTradeTopic, trade);
    }

    // trader.ctp.err.order.insert
    public void send(String uuid, CThostFtdcInputOrderField inputOrderField, CThostFtdcRspInfoField rspInfoField, String source) {
        log.debug("[send] Return error: {}", inputOrderField.getOrderRef());
        Error error = TraderCTPMapper.MAPPER.map(inputOrderField, rspInfoField, source);
        send(errOrderInsertTopic, uuid, error);
    }

    // trader.ctp.err.order.action
    public void send(String uuid, CThostFtdcInputOrderActionField inputOrderActionField, CThostFtdcRspInfoField rspInfoField, String source) {
        log.debug("[send] Return error: {}", inputOrderActionField.getOrderRef());
        Error error = TraderCTPMapper.MAPPER.map(inputOrderActionField, rspInfoField, source);
        send(errOrderActionTopic, uuid, error);
    }

    // trader.ctp.err.order.action
    public void send(String uuid, CThostFtdcOrderActionField orderActionField, CThostFtdcRspInfoField rspInfoField, String source) {
        log.debug("[send] Return error: {}", orderActionField.getOrderRef());
        Error error = TraderCTPMapper.MAPPER.map(orderActionField, rspInfoField, source);
        send(errOrderActionTopic, uuid, error);
    }

}
