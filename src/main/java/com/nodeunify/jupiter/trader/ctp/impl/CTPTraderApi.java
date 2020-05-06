package com.nodeunify.jupiter.trader.ctp.impl;

import java.util.concurrent.CompletableFuture;

import com.nodeunify.jupiter.trader.ctp.util.CTPUtil;

import org.springframework.beans.factory.annotation.Autowired;

import ctp.thosttraderapi.CThostFtdcInputOrderActionField;
import ctp.thosttraderapi.CThostFtdcInputOrderField;
import ctp.thosttraderapi.CThostFtdcInstrumentField;
import ctp.thosttraderapi.CThostFtdcInvestorPositionDetailField;
import ctp.thosttraderapi.CThostFtdcInvestorPositionField;
import ctp.thosttraderapi.CThostFtdcOrderActionField;
import ctp.thosttraderapi.CThostFtdcOrderField;
import ctp.thosttraderapi.CThostFtdcQryInstrumentField;
import ctp.thosttraderapi.CThostFtdcQryInvestorPositionDetailField;
import ctp.thosttraderapi.CThostFtdcQryInvestorPositionField;
import ctp.thosttraderapi.CThostFtdcQryOrderField;
import ctp.thosttraderapi.CThostFtdcQrySettlementInfoField;
import ctp.thosttraderapi.CThostFtdcQryTradeField;
import ctp.thosttraderapi.CThostFtdcQryTradingAccountField;
import ctp.thosttraderapi.CThostFtdcReqAuthenticateField;
import ctp.thosttraderapi.CThostFtdcReqUserLoginField;
import ctp.thosttraderapi.CThostFtdcRspAuthenticateField;
import ctp.thosttraderapi.CThostFtdcRspUserLoginField;
import ctp.thosttraderapi.CThostFtdcSettlementInfoConfirmField;
import ctp.thosttraderapi.CThostFtdcSettlementInfoField;
import ctp.thosttraderapi.CThostFtdcTradeField;
import ctp.thosttraderapi.CThostFtdcTraderApi;
import ctp.thosttraderapi.CThostFtdcTraderSpi;
import ctp.thosttraderapi.CThostFtdcTradingAccountField;
import ctp.thosttraderapi.CThostFtdcUserLogoutField;
import ctp.thosttraderapi.THOST_TE_RESUME_TYPE;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CTPTraderApi {

    @Autowired
    private CTPRequestManager ctpRequestManager;

    private CThostFtdcTraderApi traderApi;

    public CTPTraderApi(CThostFtdcTraderApi traderApi) {
        this.traderApi = traderApi;
    }

    public static CTPTraderApi createFtdcTraderApi() {
        return createFtdcTraderApi("");
    }

    public static CTPTraderApi createFtdcTraderApi(String pszFlowPath) {
        CThostFtdcTraderApi cThostFtdcTraderApi = CThostFtdcTraderApi.CreateFtdcTraderApi(pszFlowPath);
        return new CTPTraderApi(cThostFtdcTraderApi);
    }

    public void registerFront(String pszFrontAddress) {
        traderApi.RegisterFront(pszFrontAddress);
    }

    public void registerNameServer(String pszNsAddress) {
        traderApi.RegisterNameServer(pszNsAddress);
    }

    public void registerSpi(CThostFtdcTraderSpi pSpi) {
        traderApi.RegisterSpi(pSpi);
    }

    public void subscribePrivateTopic(THOST_TE_RESUME_TYPE resumeType) {
        traderApi.SubscribePrivateTopic(resumeType);
    }

    public void subscribePublicTopic(THOST_TE_RESUME_TYPE resumeType) {
        traderApi.SubscribePublicTopic(resumeType);
    }

    public CompletableFuture<Boolean> init() {
        CompletableFuture<Boolean> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(CTPUtil.REQUEST_ID_INIT, listener);
        traderApi.Init();
        log.info("[init] 发起连接交易前置机");
        return listener;
    }

    public int join() {
        return traderApi.Join();
    }

    public CompletableFuture<Boolean> release() {
        CompletableFuture<Boolean> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(CTPUtil.REQUEST_ID_RELEASE, listener);
        traderApi.Release();
        log.info("[release] 释放连接交易前置机");
        return listener;
    }

    public CompletableFuture<CThostFtdcRspAuthenticateField> reqAuthenticate(CThostFtdcReqAuthenticateField pReqAuthenticate) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        CompletableFuture<CThostFtdcRspAuthenticateField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(requestID, listener);

        if (traderApi.ReqAuthenticate(pReqAuthenticate, requestID) != 0) {
            log.error("[reqAuthenticate] 发送认证请求失败");
        } else {
            log.info("[reqAuthenticate] 发送认证请求成功");
        }

        return listener;
    }

    public CompletableFuture<CThostFtdcRspUserLoginField> reqUserLogin(CThostFtdcReqUserLoginField pReqUserLogin) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        CompletableFuture<CThostFtdcRspUserLoginField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(requestID, listener);

        String userInfo = "BorkerID=" + pReqUserLogin.getBrokerID() + ",UserID=" + pReqUserLogin.getUserID();
        if (traderApi.ReqUserLogin(pReqUserLogin, requestID) != 0) {
            log.error("[reqUserLogin] 发送登录请求失败. 用户信息:{}", userInfo);
        } else {
            log.info("[reqUserLogin] 发送登录请求成功. 用户信息:{}", userInfo);
        }

        return listener.thenApply(pRspUserLogin -> {
            ctpRequestManager.setFrontID(pRspUserLogin.getFrontID());
            ctpRequestManager.setSessionID(pRspUserLogin.getSessionID());
            ctpRequestManager.configOrderRefPrefix();
            return pRspUserLogin;
        });
    }

    public CompletableFuture<CThostFtdcUserLogoutField> reqUserLogout(CThostFtdcUserLogoutField pReqUserLogout) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        CompletableFuture<CThostFtdcUserLogoutField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(requestID, listener);

        String userInfo = "BorkerID=" + pReqUserLogout.getBrokerID() + ",UserID=" + pReqUserLogout.getUserID();
        if (traderApi.ReqUserLogout(pReqUserLogout, requestID) != 0) {
            log.error("[reqUserLogin] 发送登录请求失败. 用户信息:{}", userInfo);
        } else {
            log.info("[reqUserLogin] 发送登录请求成功. 用户信息:{}", userInfo);
        }

        return listener.thenApply(pUserLogout -> {
            // 重置
            ctpRequestManager.setFrontID(0);
            ctpRequestManager.setSessionID(0);
            return pUserLogout;
        });
    }

    public CompletableFuture<CThostFtdcSettlementInfoField> reqQrySettlementInfo(CThostFtdcQrySettlementInfoField pQrySettlementInfo) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        return reqQrySettlementInfo(pQrySettlementInfo, requestID);
    }

    public CompletableFuture<CThostFtdcSettlementInfoField> reqQrySettlementInfo(CThostFtdcQrySettlementInfoField pQrySettlementInfo, int nRequestID) {
        CompletableFuture<CThostFtdcSettlementInfoField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(nRequestID, listener);

        if (traderApi.ReqQrySettlementInfo(pQrySettlementInfo, nRequestID) != 0) {
            log.error("[reqQrySettlementInfo] 发送查询投资者结算结果请求失败");
        } else {
            log.info("[reqQrySettlementInfo] 发送查询投资者结算结果请求成功");
        }

        return listener;
    }

    public CompletableFuture<CThostFtdcSettlementInfoConfirmField> reqSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        return reqSettlementInfoConfirm(pSettlementInfoConfirm, requestID);
    }

    public CompletableFuture<CThostFtdcSettlementInfoConfirmField> reqSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm, int nRequestID) {
        CompletableFuture<CThostFtdcSettlementInfoConfirmField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(nRequestID, listener);

        if (traderApi.ReqSettlementInfoConfirm(pSettlementInfoConfirm, nRequestID) != 0) {
            log.error("[reqSettlementInfoConfirm] 发送确认投资者结算结果请求失败");
        } else {
            log.info("[reqSettlementInfoConfirm] 发送确认投资者结算结果请求成功");
        }

        return listener;
    }

    public CompletableFuture<CThostFtdcInvestorPositionField> reqQryInvestorPosition(CThostFtdcQryInvestorPositionField pQryInvestorPosition) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        return reqQryInvestorPosition(pQryInvestorPosition, requestID);
    }

    public CompletableFuture<CThostFtdcInvestorPositionField> reqQryInvestorPosition(CThostFtdcQryInvestorPositionField pQryInvestorPosition, int nRequestID) {
        CompletableFuture<CThostFtdcInvestorPositionField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(nRequestID, listener);

        if (traderApi.ReqQryInvestorPosition(pQryInvestorPosition, nRequestID) != 0) {
            log.error("[reqQryInvestorPosition] 发送持仓查询请求失败");
        } else {
            log.info("[reqQryInvestorPosition] 发送持仓查询请求成功");
        }
        
        return listener;
    }

    public CompletableFuture<CThostFtdcInvestorPositionDetailField> reqQryInvestorPositionDetail(CThostFtdcQryInvestorPositionDetailField pQryInvestorPositionDetail) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        return reqQryInvestorPositionDetail(pQryInvestorPositionDetail, requestID);
    }

    public CompletableFuture<CThostFtdcInvestorPositionDetailField> reqQryInvestorPositionDetail(CThostFtdcQryInvestorPositionDetailField pQryInvestorPositionDetail, int nRequestID) {
        CompletableFuture<CThostFtdcInvestorPositionDetailField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(nRequestID, listener);

        if (traderApi.ReqQryInvestorPositionDetail(pQryInvestorPositionDetail, nRequestID) != 0) {
            log.error("[reqQryInvestorPositionDetail] 发送请求失败");
        } else {
            log.info("[reqQryInvestorPositionDetail] 发送请求成功");
        }
        
        return listener;
    }

    public CompletableFuture<CThostFtdcTradingAccountField> reqQryTradingAccount(CThostFtdcQryTradingAccountField pQryTradingAccount) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        return reqQryTradingAccount(pQryTradingAccount, requestID);
    }

    public CompletableFuture<CThostFtdcTradingAccountField> reqQryTradingAccount(CThostFtdcQryTradingAccountField pQryTradingAccount, int nRequestID) {
        CompletableFuture<CThostFtdcTradingAccountField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(nRequestID, listener);

        if (traderApi.ReqQryTradingAccount(pQryTradingAccount, nRequestID) != 0) {
            log.error("[reqQryTradingAccount] 发送请求失败");
        } else {
            log.info("[reqQryTradingAccount] 发送请求成功");
        }
        
        return listener;
    }

    public CompletableFuture<CThostFtdcInstrumentField> reqQryInstrument(CThostFtdcQryInstrumentField pQryInstrument) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        return reqQryInstrument(pQryInstrument, requestID);
    }

    public CompletableFuture<CThostFtdcInstrumentField> reqQryInstrument(CThostFtdcQryInstrumentField pQryInstrument, int nRequestID) {
        CompletableFuture<CThostFtdcInstrumentField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(nRequestID, listener);

        if (traderApi.ReqQryInstrument(pQryInstrument, nRequestID) != 0) {
            log.error("[reqQryInstrument] 发送查询合约请求失败");
        } else {
            log.info("[reqQryInstrument] 发送查询合约请求成功");
        }

        return listener;
    }

    public CompletableFuture<CThostFtdcOrderField> reqQryOrder(CThostFtdcQryOrderField pQryTrade) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        return reqQryOrder(pQryTrade, requestID);
    }

    public CompletableFuture<CThostFtdcOrderField> reqQryOrder(CThostFtdcQryOrderField pQryOrder, int nRequestID) {
        CompletableFuture<CThostFtdcOrderField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(nRequestID, listener);

        if (traderApi.ReqQryOrder(pQryOrder, nRequestID) != 0) {
            log.error("[reqQryOrder] 发送请求失败");
        } else {
            log.info("[reqQryOrder] 发送请求成功");
        }

        return listener;
    }

    public CompletableFuture<CThostFtdcTradeField> reqQryTrade(CThostFtdcQryTradeField pQryTrade) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        return reqQryTrade(pQryTrade, requestID);
    }

    public CompletableFuture<CThostFtdcTradeField> reqQryTrade(CThostFtdcQryTradeField pQryTrade, int nRequestID) {
        CompletableFuture<CThostFtdcTradeField> listener = new CompletableFuture<>();
        ctpRequestManager.addListener(nRequestID, listener);

        if (traderApi.ReqQryTrade(pQryTrade, nRequestID) != 0) {
            log.error("[reqQryTrade] 发送请求失败");
        } else {
            log.info("[reqQryTrade] 发送请求成功");
        }

        return listener;
    }

    public CompletableFuture<CThostFtdcOrderField> reqOrderInsert(CThostFtdcInputOrderField pInputOrder) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        return reqOrderInsert(pInputOrder, requestID);
    }

    public CompletableFuture<CThostFtdcOrderField> reqOrderInsert(CThostFtdcInputOrderField pInputOrder, int nRequestID) {
        CompletableFuture<CThostFtdcOrderField> successFuture = new CompletableFuture<>();
        ctpRequestManager.addOrderRefListener(nRequestID, successFuture);
        CompletableFuture<CThostFtdcInputOrderField> errorFuture = new CompletableFuture<>();
        ctpRequestManager.addListener(nRequestID, errorFuture);
        if (traderApi.ReqOrderInsert(pInputOrder, nRequestID) != 0) {
            log.error("[reqOrderInsert] 发送报单请求失败");
        } else {
            log.info("[reqOrderInsert] 发送报单请求成功");
        }

        return CompletableFuture
            .anyOf(successFuture, errorFuture)
            .thenApply(object -> (CThostFtdcOrderField) object);
    }

    public CompletableFuture<CThostFtdcOrderActionField> reqOrderAction(CThostFtdcInputOrderActionField pInputOrderAction) {
        int requestID = ctpRequestManager.getAndIncrementRequestID();
        return reqOrderAction(pInputOrderAction, requestID);
    }

    public CompletableFuture<CThostFtdcOrderActionField> reqOrderAction(CThostFtdcInputOrderActionField pInputOrderAction, int nRequestID) {
        CompletableFuture<CThostFtdcOrderField> successFuture = new CompletableFuture<>();
        ctpRequestManager.addOrderRefListener(nRequestID, successFuture);
        CompletableFuture<CThostFtdcInputOrderActionField> errorFuture = new CompletableFuture<>();
        ctpRequestManager.addListener(nRequestID, errorFuture);
        if (traderApi.ReqOrderAction(pInputOrderAction, nRequestID) != 0) {
            log.error("[reqOrderAction] 发送撤单请求失败");
        } else {
            log.info("[reqOrderAction] 发送撤单请求成功");
        }

        return CompletableFuture
            .anyOf(successFuture, errorFuture)
            .thenApply(object -> (CThostFtdcOrderActionField) object);
    }
}
