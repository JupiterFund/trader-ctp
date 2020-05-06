package com.nodeunify.jupiter.trader.ctp.impl;

import ctp.thosttraderapi.CThostFtdcInputOrderActionField;
import ctp.thosttraderapi.CThostFtdcInputOrderField;
import ctp.thosttraderapi.CThostFtdcInstrumentField;
import ctp.thosttraderapi.CThostFtdcInvestorPositionDetailField;
import ctp.thosttraderapi.CThostFtdcInvestorPositionField;
import ctp.thosttraderapi.CThostFtdcOrderActionField;
import ctp.thosttraderapi.CThostFtdcOrderField;
import ctp.thosttraderapi.CThostFtdcRspAuthenticateField;
import ctp.thosttraderapi.CThostFtdcRspInfoField;
import ctp.thosttraderapi.CThostFtdcRspUserLoginField;
import ctp.thosttraderapi.CThostFtdcSettlementInfoConfirmField;
import ctp.thosttraderapi.CThostFtdcSettlementInfoField;
import ctp.thosttraderapi.CThostFtdcTradeField;
import ctp.thosttraderapi.CThostFtdcTraderSpi;
import ctp.thosttraderapi.CThostFtdcTradingAccountField;
import ctp.thosttraderapi.CThostFtdcUserLogoutField;

public class CTPTraderSpiAdapter extends CThostFtdcTraderSpi {

    private CTPTraderSpi delegate;

    public CTPTraderSpiAdapter(CTPTraderSpi traderSpi) {
        this.delegate = traderSpi;
    }

    @Override
    public void OnFrontConnected() {
        delegate.onFrontConnected();
    }

    @Override
    public void OnFrontDisconnected(int nReason) {
        delegate.onFrontDisconnected(nReason);
    }

    @Override
    public void OnHeartBeatWarning(int nTimeLapse) {
        delegate.onHeartBeatWarning(nTimeLapse);
    }

    @Override
    public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo,
            int nRequestID, boolean bIsLast) {
        delegate.onRspAuthenticate(pRspAuthenticateField, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo,
            int nRequestID, boolean isLast) {
        delegate.onRspUserLogin(pRspUserLogin, pRspInfo, nRequestID, isLast);
    }

    @Override
    public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID,
            boolean isLast) {
        delegate.onRspUserLogout(pUserLogout, pRspInfo, nRequestID, isLast);
    }

    @Override
    public void OnRspQrySettlementInfo(CThostFtdcSettlementInfoField pSettlementInfo, CThostFtdcRspInfoField pRspInfo,
            int nRequestID, boolean bIsLast) {
        delegate.onRspQrySettlementInfo(pSettlementInfo, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm,
            CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        delegate.onRspSettlementInfoConfirm(pSettlementInfoConfirm, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnRspQryInvestorPosition(CThostFtdcInvestorPositionField pInvestorPosition,
            CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        delegate.onRspQryInvestorPosition(pInvestorPosition, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnRspQryInvestorPositionDetail(CThostFtdcInvestorPositionDetailField pInvestorPositionDetail,
            CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        delegate.onRspQryInvestorPositionDetail(pInvestorPositionDetail, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnRspQryTradingAccount(CThostFtdcTradingAccountField pTradingAccount, CThostFtdcRspInfoField pRspInfo,
            int nRequestID, boolean bIsLast) {
        delegate.onRspQryTradingAccount(pTradingAccount, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnRspQryInstrument(CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo,
            int nRequestID, boolean bIsLast) {
        delegate.onRspQryInstrument(pInstrument, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnRspQryOrder(CThostFtdcOrderField pOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, 
            boolean bIsLast) {
        delegate.onRspQryOrder(pOrder, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnRspQryTrade(CThostFtdcTradeField pTrade, CThostFtdcRspInfoField pRspInfo, int nRequestID, 
            boolean bIsLast) {
        delegate.onRspQryTrade(pTrade, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnRspOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID,
            boolean bIsLast) {
        delegate.onRspOrderInsert(pInputOrder, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnErrRtnOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo) {
        delegate.onErrRtnOrderInsert(pInputOrder, pRspInfo);
    }

    @Override
    public void OnRspOrderAction(CThostFtdcInputOrderActionField pInputOrderAction, CThostFtdcRspInfoField pRspInfo,
            int nRequestID, boolean bIsLast) {
        delegate.onRspOrderAction(pInputOrderAction, pRspInfo, nRequestID, bIsLast);
    }

    @Override
    public void OnErrRtnOrderAction(CThostFtdcOrderActionField pOrderAction, CThostFtdcRspInfoField pRspInfo) {
        delegate.onErrRtnOrderAction(pOrderAction, pRspInfo);
    }

    @Override
    public void OnRtnOrder(CThostFtdcOrderField pOrder) {
        delegate.onRtnOrder(pOrder);
    }

    @Override
    public void OnRtnTrade(CThostFtdcTradeField pTrade) {
        delegate.onRtnTrade(pTrade);
    }
    
}
