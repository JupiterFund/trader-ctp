package com.nodeunify.jupiter.trader.ctp;

import java.util.concurrent.CompletableFuture;

import javax.annotation.PreDestroy;

import com.nodeunify.jupiter.trader.ctp.impl.CTPTraderApi;
import com.nodeunify.jupiter.trader.ctp.impl.CTPTraderSpi;
import com.nodeunify.jupiter.trader.ctp.impl.CTPTraderSpiAdapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Service;

import ctp.thosttraderapi.CThostFtdcInstrumentField;
import ctp.thosttraderapi.CThostFtdcQryInstrumentField;
import ctp.thosttraderapi.CThostFtdcQrySettlementInfoField;
import ctp.thosttraderapi.CThostFtdcReqAuthenticateField;
import ctp.thosttraderapi.CThostFtdcReqUserLoginField;
import ctp.thosttraderapi.CThostFtdcRspAuthenticateField;
import ctp.thosttraderapi.CThostFtdcRspUserLoginField;
import ctp.thosttraderapi.CThostFtdcSettlementInfoConfirmField;
import ctp.thosttraderapi.CThostFtdcSettlementInfoField;
import ctp.thosttraderapi.CThostFtdcTraderSpi;
import ctp.thosttraderapi.CThostFtdcUserLogoutField;
import ctp.thosttraderapi.THOST_TE_RESUME_TYPE;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CTPTrader {

    static {
        System.loadLibrary("thosttraderapi_se");
        System.loadLibrary("thosttraderapi_wrap");
    }

    @Autowired
    private CTPTraderApi traderApi;
    @Autowired
    private CTPTraderSpi traderSpi;
    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Value("#{'tcp://' + '${app.ctp.ip}' + ':' + '${app.ctp.port}'}")
    private String ctpTradeAddress;
    // Prod
    // private final static String ctpTradeAddress = "tcp://180.166.132.67:41205";
    @Value("${app.ctp.username}")
    private String userId;
    @Value("${app.ctp.password}")
    private String password;
    @Value("${app.ctp.investor-id}")
    private String investorId;
    @Value("${app.ctp.account-id}")
    private String accountId;
    @Value("${app.ctp.app-id}")
    private String appId;
    @Value("${app.ctp.auth-code}")
    private String authCode;
    @Value("${app.ctp.broker-id}")
    private String brokerId;

    @PreDestroy
    public void preDestroy() {
        logout().thenComposeAsync(nil -> traderApi.release());
    }

    public void start() {
        // CTP API必须在这个方法中被初始化，不能放在PostConstruct方法里。
        // 因为CTP API要求挂起主线程，会导致SpringBoot应用无法完全启动。
        CThostFtdcTraderSpi spiAdapter = new CTPTraderSpiAdapter(traderSpi);
        traderApi.registerSpi(spiAdapter);
        traderApi.registerFront(ctpTradeAddress);
        traderApi.subscribePublicTopic(THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
        traderApi.subscribePrivateTopic(THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
        // Init方法必须被异步执行，否则容易导致由于swig引起的线程错误。
        traderApi.init()
            .thenCompose(nil -> authenticate())
            .thenCompose(nil -> login())
            .thenComposeAsync(nil -> queryInstrument())
            .thenComposeAsync(nil ->  CompletableFuture.allOf(querySettlementInfo(), confirmSettlementInfo()))
            .thenAcceptAsync(nil -> {
                // 打开所有Kafka订阅
                registry.getAllListenerContainers()
                    .parallelStream()
                    .forEach(listener -> listener.start());
            });
        log.info("[start] CTP接口异步连接中，主进程挂起");
        traderApi.join();
        // 不要在traderAPI以外的地方使用swig object，有异步时效性，容易造成错误。
    }

    private CompletableFuture<CThostFtdcRspAuthenticateField> authenticate() {
        CThostFtdcReqAuthenticateField reqAuthenticateField = new CThostFtdcReqAuthenticateField();
        reqAuthenticateField.setBrokerID(brokerId);
        reqAuthenticateField.setUserID(userId);
        reqAuthenticateField.setAppID(appId);
        reqAuthenticateField.setAuthCode(authCode);
        return traderApi.reqAuthenticate(reqAuthenticateField);
    }

    private CompletableFuture<CThostFtdcRspUserLoginField> login() {
        CThostFtdcReqUserLoginField reqUserLoginField = new CThostFtdcReqUserLoginField();
        reqUserLoginField.setBrokerID(brokerId);
        reqUserLoginField.setUserID(userId);
        reqUserLoginField.setPassword(password);
        return traderApi.reqUserLogin(reqUserLoginField);
    }

    private CompletableFuture<CThostFtdcUserLogoutField> logout() {
        CThostFtdcUserLogoutField reqUserLogoutField = new CThostFtdcUserLogoutField();
        reqUserLogoutField.setBrokerID(brokerId);
        reqUserLogoutField.setUserID(userId);
        return traderApi.reqUserLogout(reqUserLogoutField);
    }

    private CompletableFuture<CThostFtdcInstrumentField> queryInstrument() {
        CThostFtdcQryInstrumentField qryInstrumentField = new CThostFtdcQryInstrumentField();
        qryInstrumentField.setExchangeID("");
        return traderApi.reqQryInstrument(qryInstrumentField);
    }

    private CompletableFuture<CThostFtdcSettlementInfoField> querySettlementInfo() {
        CThostFtdcQrySettlementInfoField qrySettlementInfoField = new CThostFtdcQrySettlementInfoField();
        qrySettlementInfoField.setBrokerID(brokerId);
        qrySettlementInfoField.setInvestorID(investorId);
        qrySettlementInfoField.setAccountID(accountId);
        qrySettlementInfoField.setCurrencyID("CNY");
        qrySettlementInfoField.setTradingDay("20200413");
        return traderApi.reqQrySettlementInfo(qrySettlementInfoField);
    }

    private CompletableFuture<CThostFtdcSettlementInfoConfirmField> confirmSettlementInfo() {
        CThostFtdcSettlementInfoConfirmField settlementInfoConfirmField = new CThostFtdcSettlementInfoConfirmField();
        settlementInfoConfirmField.setBrokerID(brokerId);
        settlementInfoConfirmField.setInvestorID(investorId);
        return traderApi.reqSettlementInfoConfirm(settlementInfoConfirmField);
    }
}
