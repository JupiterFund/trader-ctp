package com.nodeunify.jupiter.trader.ctp.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.nodeunify.jupiter.trader.ctp.datatype.ThostFtdcUserApiDataTypeLibrary;
import com.nodeunify.jupiter.trader.ctp.kafka.KafkaProducer;
import com.nodeunify.jupiter.trader.ctp.util.CTPUtil;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ctp.thosttraderapi.CThostFtdcDepthMarketDataField;
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
import ctp.thosttraderapi.CThostFtdcTradingAccountField;
import ctp.thosttraderapi.CThostFtdcUserLogoutField;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unchecked")
@Slf4j
public class CTPTraderSpi {

    @Autowired
    private CTPRequestManager ctpRequestManager;
    @Autowired
    private KafkaProducer kafkaProducer;

    private final Map<Integer, List<?>> responseMap = new ConcurrentHashMap<>();

    /**
     * 当客户端与交易后台建立起通信连接时（还未登录前）,该方法被调用。
     */
    public void onFrontConnected() {
        log.info("[onFrontConnected] 连接交易前置机成功!");
        CompletableFuture<Boolean> listener = (CompletableFuture<Boolean>) ctpRequestManager
                .getListener(CTPUtil.REQUEST_ID_INIT);
        if (listener != null && !listener.isDone()) {
            listener.complete(true);
        }
    }

    /**
     * 当客户端与交易后台通信连接断开时,该方法被调用。当发生这个情况后,API会自动重新 连接,客户端可不做处理。
     * 
     * 0x1001 网络读失败 0x1002 网络写失败 0x2001 接收心跳超时 0x2002 发送心跳失败 0x2003 收到错误报文
     * 
     * @param nReason
     */
    public void onFrontDisconnected(int nReason) {
        CompletableFuture<Boolean> listener = (CompletableFuture<Boolean>) ctpRequestManager
                .getListener(CTPUtil.REQUEST_ID_RELEASE);
        if (listener != null && !listener.isDone()) {
            // 主动断开，FutureRegistry中留有已保存的CompletableFuture
            log.info("[onFrontDisconnected] 客户端与交易服务断开连接. 原因:{}", CTPUtil.getReasonTraderMsg(nReason));
            listener.complete(true);
        } else {
            // 自动中断，不存在CompletableFuture
            log.error("[onFrontDisconnected] 客户端与交易服务断开连接. 原因:{}", CTPUtil.getReasonTraderMsg(nReason));
        }
    }

    /**
     * 永远不会发生，已经对 API 用户屏蔽了该响应
     * 
     * @param nTimeLapse
     */
    public void onHeartBeatWarning(int nTimeLapse) {
        log.warn("[onHeartBeatWarning] 心跳超时{}秒,警告!", nTimeLapse);
    }

    /**
     * 认证请求响应
     * 
     * @param pRspAuthenticateField
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo,
            int nRequestID, boolean bIsLast) {
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspAuthenticate] 交易服务器认证失败. 错误代码:{}; 错误消息:{}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspAuthenticate] 交易服务器认证成功");
        }

        CompletableFuture<CThostFtdcRspAuthenticateField> listener = (CompletableFuture<CThostFtdcRspAuthenticateField>) ctpRequestManager
                .getListener(nRequestID);
        if (listener != null && !listener.isDone()) {
            listener.complete(pRspAuthenticateField);
        }
    }

    /**
     * 登录请求响应
     * 
     * @param pRspUserLogin
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo,
            int nRequestID, boolean isLast) {
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspUserLogin] 交易服务器登录失败. 错误代码:{}; 错误消息:{}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspUserLogin] 交易服务器登录成功");
            log.info("[onRspUserLogin] 登录信息: FrontID={},SessionID={}", pRspUserLogin.getFrontID(),
                    pRspUserLogin.getSessionID());
        }

        CompletableFuture<CThostFtdcRspUserLoginField> listener = (CompletableFuture<CThostFtdcRspUserLoginField>) ctpRequestManager
                .getListener(nRequestID);
        if (listener != null && !listener.isDone()) {
            listener.complete(pRspUserLogin);
        }
    }

    /**
     * 注销请求响应
     * 
     * @param pUserLogout
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID,
            boolean isLast) {
        log.info("[onRspUserLogout] 交易服务器注销成功");

        CompletableFuture<CThostFtdcUserLogoutField> listener = (CompletableFuture<CThostFtdcUserLogoutField>) ctpRequestManager
                .getListener(nRequestID);
        if (listener != null && !listener.isDone()) {
            listener.complete(pUserLogout);
        }
    }

    /**
     * 查询结算单响应
     * 
     * @param pSettlementInfo
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspQrySettlementInfo(CThostFtdcSettlementInfoField pSettlementInfo, CThostFtdcRspInfoField pRspInfo,
            int nRequestID, boolean bIsLast) {
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspQrySettlementInfo] 查询投资者结算结果响应失败. 错误代码:{}; 错误消息:{}", pRspInfo.getErrorID(),
                    pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspQrySettlementInfo] 查询投资者结算结果响应成功");
        }

        CompletableFuture<CThostFtdcSettlementInfoField> listener = (CompletableFuture<CThostFtdcSettlementInfoField>) ctpRequestManager
                .getListener(nRequestID);
        if (listener != null && !listener.isDone()) {
            listener.complete(pSettlementInfo);
        }
    }

    /**
     * 确认结算单响应
     * 
     * @param pSettlementInfoConfirm
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm,
            CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspSettlementInfoConfirm] 确认投资者结算结果响应失败. 错误代码:{}; 错误消息:{}", pRspInfo.getErrorID(),
                    pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspSettlementInfoConfirm] 确认投资者结算结果响应成功");
        }

        CompletableFuture<CThostFtdcSettlementInfoConfirmField> listener = (CompletableFuture<CThostFtdcSettlementInfoConfirmField>) ctpRequestManager
                .getListener(nRequestID);
        if (listener != null && !listener.isDone()) {
            listener.complete(pSettlementInfoConfirm);
        }
    }

    /**
     * 投资者持仓查询应答
     * 
     * @param pInvestorPosition
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspQryInvestorPosition(CThostFtdcInvestorPositionField pInvestorPosition,
            CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspQryInvestorPosition] 投资者持仓查询应答失败. 错误代码:{}; 错误消息:{}", pRspInfo.getErrorID(),
                    pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspQryInvestorPosition] 投资者持仓查询应答成功");
        }

        List<CThostFtdcInvestorPositionField> fields = (List<CThostFtdcInvestorPositionField>) responseMap.getOrDefault(nRequestID, new ArrayList<>());
        // TODO: 返回结果含有多次回调时，在同步处理的情况下，
        // 需要一次性返回整个结果集. 暂时使用Map来缓存结果。
        // 最终采用RxJava响应式回调来处理异步结果集.
        // InvestorPosition can be null if not found
        if (pInvestorPosition != null) {
            log.debug(
                "[onRspQryInvestorPosition] 合约代码:{}; 交易所代码:{}; 经纪公司代码:{}; 投资者代码:{}; 多空方向:{}; 今日持仓:{}; 上日持仓:{}; 开仓量:{}; 平仓量:{}; 开仓金额:{}; 平仓金额:{}; 持仓成本:{}",
                pInvestorPosition.getInstrumentID(), pInvestorPosition.getExchangeID(),
                pInvestorPosition.getBrokerID(), pInvestorPosition.getInvestorID(),
                pInvestorPosition.getPosiDirection(), pInvestorPosition.getPosition(),
                pInvestorPosition.getYdPosition(), pInvestorPosition.getOpenVolume(),
                pInvestorPosition.getCloseVolume(), pInvestorPosition.getOpenAmount(),
                pInvestorPosition.getCloseAmount(), pInvestorPosition.getPositionCost());

            // 回调结果会被下一次回调覆盖. 临时解决办法:复制拷贝一份回调结果放入缓存
            CThostFtdcInvestorPositionField copy = new CThostFtdcInvestorPositionField();
            BeanUtils.copyProperties(pInvestorPosition, copy);
            fields.add(copy);
            responseMap.put(nRequestID, fields);
        }

        if (bIsLast) {
            log.debug("[onRspQryInvestorPosition] 最后一条持仓记录");
            CompletableFuture<List<CThostFtdcInvestorPositionField>> listener = 
                (CompletableFuture<List<CThostFtdcInvestorPositionField>>) ctpRequestManager.getListener(nRequestID);
            if (listener != null && !listener.isDone()) {
                listener.complete(fields);
            }
        }

        if (pInvestorPosition != null) {
            String uuid = ctpRequestManager.lookupUUID(nRequestID);
            kafkaProducer.send(uuid, pInvestorPosition);
        }
    }

    /**
     * 请求查询投资者持仓明细响应
     * 
     * @param pInvestorPositionDetail
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspQryInvestorPositionDetail(CThostFtdcInvestorPositionDetailField pInvestorPositionDetail, 
            CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspQryInvestorPositionDetail] 查询投资者持仓明细响应失败. 错误代码:{}; 错误消息:{}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspQryInvestorPositionDetail] 查询投资者持仓明细响应成功");
        }

        List<CThostFtdcInvestorPositionDetailField> fields = (List<CThostFtdcInvestorPositionDetailField>) responseMap.getOrDefault(nRequestID, new ArrayList<>());

        if (pInvestorPositionDetail != null) {
            log.debug(
                "[onRspQryInvestorPositionDetail] 合约代码:{}; 交易所代码:{}; 经纪公司代码:{}; 投资者代码:{}.",
                pInvestorPositionDetail.getInstrumentID(), pInvestorPositionDetail.getExchangeID(),
                pInvestorPositionDetail.getBrokerID(), pInvestorPositionDetail.getInvestorID());

            CThostFtdcInvestorPositionDetailField copy = new CThostFtdcInvestorPositionDetailField();
            BeanUtils.copyProperties(pInvestorPositionDetail, copy);
            fields.add(copy);
            responseMap.put(nRequestID, fields);
        }

        if (bIsLast) {
            log.debug("[onRspQryInvestorPositionDetail] 最后一条持仓明细记录");
            CompletableFuture<List<CThostFtdcInvestorPositionDetailField>> listener = 
                (CompletableFuture<List<CThostFtdcInvestorPositionDetailField>>) ctpRequestManager.getListener(nRequestID);
            if (listener != null && !listener.isDone()) {
                listener.complete(fields);
            }
        }
    }

    /**
     * 请求查询资金账户响应
     * 
     * @param pTradingAccount
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspQryTradingAccount(CThostFtdcTradingAccountField pTradingAccount, CThostFtdcRspInfoField pRspInfo, 
            int nRequestID, boolean bIsLast) {
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspQryTradingAccount] 查询资金账户响应失败. 错误代码:{}; 错误消息:{}", 
                pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspQryTradingAccount] 查询资金账户响应成功");
        }
        
        log.info("[onRspQryTradingAccount] 可用资金:{}; 期货结算准备金:{}; 持仓盈亏:{}; 平仓盈亏:{};", 
            pTradingAccount.getAvailable(), pTradingAccount.getBalance(), pTradingAccount.getPositionProfit(), 
            pTradingAccount.getCloseProfit());

        CompletableFuture<CThostFtdcTradingAccountField> listener = 
                (CompletableFuture<CThostFtdcTradingAccountField>) ctpRequestManager.getListener(nRequestID);
        if (listener != null && !listener.isDone()) {
            listener.complete(pTradingAccount);
        }

        String uuid = ctpRequestManager.lookupUUID(nRequestID);
        kafkaProducer.send(uuid, pTradingAccount);
    }

    /**
     * 请求查询合约响应
     * 
     * @param pInstrument
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspQryInstrument(CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo, 
            int nRequestID, boolean bIsLast) {
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspQryInstrument] 查询合约响应失败. 错误代码:{}; 错误消息:{}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspQryInstrument] 查询合约响应成功");
        }

        if (bIsLast) {
            log.debug("[onRspQryInstrument] 最后一条合约记录");
            CompletableFuture<CThostFtdcInstrumentField> listener = 
                    (CompletableFuture<CThostFtdcInstrumentField>) ctpRequestManager.getListener(nRequestID);
            if (listener != null && !listener.isDone()) {
                listener.complete(pInstrument);
            }
        }

        String uuid = ctpRequestManager.lookupUUID(nRequestID);
        kafkaProducer.send(uuid, pInstrument);
    }

    /**
     * 
     * 
     * @param pDepthMarketData
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspQryDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData, CThostFtdcRspInfoField pRspInfo, 
            int nRequestID, boolean bIsLast) {
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspQryDepthMarketData] 查询行情数据响应失败. 错误代码:{}; 错误消息:{}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspQryDepthMarketData] 查询行情数据响应成功");
        }

        CompletableFuture<CThostFtdcDepthMarketDataField> listener = 
                (CompletableFuture<CThostFtdcDepthMarketDataField>) ctpRequestManager.getListener(nRequestID);
        if (listener != null && !listener.isDone()) {
            listener.complete(pDepthMarketData);
        }
    }

    /**
     * 报单查询响应
     * 
     * @param pOrder
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspQryOrder(CThostFtdcOrderField pOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, 
            boolean bIsLast) {
        log.debug("{}", pOrder.getStatusMsg());
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspQryOrder] 报单查询响应失败. 错误代码:{}; 错误消息:{}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspQryOrder] 报单查询响应成功");
        }
        
        CompletableFuture<CThostFtdcOrderField> listener = 
                (CompletableFuture<CThostFtdcOrderField>) ctpRequestManager.getListener(nRequestID);
        if (listener != null && !listener.isDone()) {
            listener.complete(pOrder);
        }

        String uuid = ctpRequestManager.lookupUUID(nRequestID);
        kafkaProducer.send(uuid, pOrder);
    }

    /**
     * 成交单查询应答
     * 
     * @param pTrade
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspQryTrade(CThostFtdcTradeField pTrade, CThostFtdcRspInfoField pRspInfo, int nRequestID, 
            boolean bIsLast) {
        log.debug("{}", pTrade.getPrice());
        if (null != pRspInfo && pRspInfo.getErrorID() != 0) {
            log.error("[onRspQryTrade] 成交单查询应答失败. 错误代码:{}; 错误消息:{}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        } else {
            log.info("[onRspQryTrade] 成交单查询应答成功");
        }
        
        CompletableFuture<CThostFtdcTradeField> listener = 
                (CompletableFuture<CThostFtdcTradeField>) ctpRequestManager.getListener(nRequestID);
        if (listener != null && !listener.isDone()) {
            listener.complete(pTrade);
        }

        String uuid = ctpRequestManager.lookupUUID(nRequestID);
        kafkaProducer.send(uuid, pTrade);
    }

    /**
     * 报单录入应答。当客户端发出过报单录入指令后，交易托管系统返回响应时，该方法会被
     * 调用。
     * 
     * @param pInputOrder
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, 
            boolean bIsLast) {
        log.info("[onRspOrderInsert]: {}, {}, {}", pInputOrder.getOrderRef(), pRspInfo.getErrorID(), 
            pRspInfo.getErrorMsg());

        CompletableFuture<CThostFtdcInputOrderField> errorListener = 
                (CompletableFuture<CThostFtdcInputOrderField>) ctpRequestManager.getListener(nRequestID);
        // CTP校验未通过
        if (errorListener != null && !errorListener.isDone()) {
            errorListener.completeExceptionally(new RuntimeException("CTP验证未通过"));
        }

        String uuid = ctpRequestManager.lookupUUID(Integer.parseInt(pInputOrder.getOrderRef()));
        kafkaProducer.send(uuid, pInputOrder, pRspInfo, "front");
    }

    /**
     * 报单录入错误回报。由交易托管系统主动通知客户端，该方法会被调用。
     * 当被CTP拒单时，该客户名下所有的链接都会收到的回调。
     * 
     * @param pInputOrder
     * @param pRspInfo
     */
    public void onErrRtnOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo) {
        log.error("[onErrRtnOrderInsert]: {}, {}, {}", pInputOrder.getOrderRef(), pRspInfo.getErrorID(), 
            pRspInfo.getErrorMsg());

        String uuid = ctpRequestManager.lookupUUID(Integer.parseInt(pInputOrder.getOrderRef()));
        kafkaProducer.send(uuid, pInputOrder, pRspInfo, "exchange");
    }

    /**
     * 报单操作应答。报单操作包括报单的撤销、报单的挂起、报单的激活、报单的修改。当客
     * 户端发出过报单操作指令后，交易托管系统返回响应时，该方法会被调用。
     * 
     * @param pInputOrder
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    public void onRspOrderAction(CThostFtdcInputOrderActionField pInputOrderAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, 
            boolean bIsLast) {
        log.info("[onRspOrderAction]: {}, {}, {}", pInputOrderAction.getOrderActionRef(), pRspInfo.getErrorID(), 
            pRspInfo.getErrorMsg());

        CompletableFuture<CThostFtdcInputOrderField> errorListener = 
                (CompletableFuture<CThostFtdcInputOrderField>) ctpRequestManager.getListener(nRequestID);
        // CTP校验未通过
        if (errorListener != null && !errorListener.isDone()) {
            errorListener.completeExceptionally(new Error(pRspInfo.getErrorMsg()));
        }

        String uuid = ctpRequestManager.lookupUUID(pInputOrderAction.getOrderActionRef());
        kafkaProducer.send(uuid, pInputOrderAction, pRspInfo, "front");
    }

    /**
     * 报价操作错误回报。由交易托管系统主动通知客户端，该方法会被调用。
     * 
     * @param pInputOrder
     * @param pRspInfo
     */
    public void onErrRtnOrderAction(CThostFtdcOrderActionField pOrderAction, CThostFtdcRspInfoField pRspInfo) {
        log.error("[onErrRtnOrderAction]: {}, {}, {}", pOrderAction.getOrderRef(), pRspInfo.getErrorID(), 
            pRspInfo.getErrorMsg());

        String uuid = ctpRequestManager.lookupUUID(pOrderAction.getOrderActionRef());
        kafkaProducer.send(uuid, pOrderAction, pRspInfo, "exchange");
    }

    /**
     * 报单回报。当客户端进行报单录入、报单操作及其它原因（如部分成交）导致报单状态发
     * 生变化时，交易托管系统会主动通知客户端，该方法会被调用。
     * 
     * @param pOrder
     */
    public void onRtnOrder(CThostFtdcOrderField pOrder) {
        String statusMsg = pOrder.getStatusMsg();
        char ordeSubmitStatus = pOrder.getOrderSubmitStatus();
        char orderStatus = pOrder.getOrderStatus();
        String orderID = pOrder.getFrontID() + "_" + pOrder.getSessionID() + "_" + pOrder.getOrderRef();
        String exchangeID = pOrder.getExchangeID();
        String orderSysID = pOrder.getOrderSysID();
        String orderRef = pOrder.getOrderRef();
        log.debug("[onRtnOrder] 报单标识号:{}; StatusMsg:{}; OrdeSubmitStatus:{}; OrderStatus:{}", orderID, statusMsg, ordeSubmitStatus, orderStatus);
        orderRef = "88880";
        CompletableFuture<CThostFtdcOrderField> listener = 
                (CompletableFuture<CThostFtdcOrderField>) ctpRequestManager.getOrderRefListener(orderRef);
        switch (ordeSubmitStatus) {
            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OSS_InsertSubmitted:
                // 第一次回调：报单已提交至交易所，尚未通过审核，等候交易所报单编号。不做任何处理
                // 第二次回调：报单已通过交易所审核，获得交易所报单编号
                // 第三次可能回调：报单已跳过THOST_FTDC_OSS_Accepted状态，直接成交。
                log.info("[onRtnOrder] 报单{}已提交至交易所. 交易所代码:{}; 交易所报单编号:{}", orderID, exchangeID, orderSysID);
                if (orderSysID != null && !orderSysID.isEmpty()) {
                    if (listener != null && !listener.isDone()) {
                        listener.complete(pOrder);
                    }
                }
                break;

            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OSS_CancelSubmitted:
                // 撤单已提交
                log.info("[onRtnOrder] 撤单{}已提交至交易所. 交易所代码:{}; 交易所报单编号:{}", orderID, exchangeID, orderSysID);
                if (orderSysID != null && !orderSysID.isEmpty()) {
                    if (listener != null && !listener.isDone()) {
                        listener.complete(pOrder);
                    }
                }
                break;

            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OSS_ModifySubmitted:
                // 修改已提交
                log.info("[onRtnOrder] 报单修改{}已提交至交易所. 交易所代码:{}; 交易所报单编号:{}", orderID, exchangeID, orderSysID);
                if (orderSysID != null && !orderSysID.isEmpty()) {
                    if (listener != null && !listener.isDone()) {
                        listener.complete(pOrder);
                    }
                }
                break;

            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OSS_Accepted:
                // 报单已提交至交易所，并已通过审核，但尚未成交 
                // 主动撤单
                log.info("[onRtnOrder] {}已通过交易所审核. 交易所代码:{}; 交易所报单编号:{}", orderID, exchangeID, orderSysID);
                if (orderSysID != null && !orderSysID.isEmpty()) {
                    if (listener != null && !listener.isDone()) {
                        listener.complete(pOrder);
                    }
                }
                break;

            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OSS_InsertRejected:
                // 报单被拒绝(被动撤单)
                log.error("[onRtnOrder] 报单被交易所拒绝. 具体原因:{}", statusMsg);
                if (listener != null && !listener.isDone()) {
                    listener.completeExceptionally(null);
                }
                break;

            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OSS_CancelRejected:
                // 撤单被拒绝
                log.error("[onRtnOrder] 撤单被交易所拒绝. 具体原因:{}", statusMsg);
                if (listener != null && !listener.isDone()) {
                    listener.completeExceptionally(null);
                }
                break;

            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OSS_ModifyRejected:
                // 修改被拒绝
                log.error("[onRtnOrder] 报单修改被交易所拒绝. 具体原因:{}", statusMsg);
                if (listener != null && !listener.isDone()) {
                    listener.completeExceptionally(null);
                }
                break;
        
            default:
                break;
        }

        switch (orderStatus) {
            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OST_Unknown:
                // 未知
                // 注意: 如果该笔订单报到交易所并直接成交，
                // 状态也可能为THOST_FTDC_OST_Unknown
                if (orderSysID == null || orderSysID.isEmpty()) {
                    log.info("[onRtnOrder] 报单{}当前状态:已提交，等待审核中", orderID);
                } else {
                    log.info("[onRtnOrder] 报单{}当前状态:已提交，并且已通过审核", orderID);
                }
                break;

            // TODO: 更多状态验证
            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OST_NoTradeQueueing:
                // 未成交还在队列中
                // 第二次被调用，报单成功提交到交易所

            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OST_PartTradedQueueing:
                // 部分成交还在队列中

            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OST_AllTraded:
                // 全部成交

            case ThostFtdcUserApiDataTypeLibrary.THOST_FTDC_OST_Canceled:
                // 撤单

            default:
                log.info("[onRtnOrder] 报单{}当前状态:{}", orderID, statusMsg);
                break;
        }

        String uuid = ctpRequestManager.lookupUUID(Integer.parseInt(pOrder.getOrderRef()));
        kafkaProducer.sendReturnOrder(uuid, pOrder);
    }

    /**
     * 成交回报。当发生成交时交易托管系统会通知客户端，该方法会被调用。
     * 
     * @param pTrade
     */
    public void onRtnTrade(CThostFtdcTradeField pTrade) {
        log.info("[onRtnTrade] 报单编号{}已成交. 交易所代码:{}; 交易所交易员代码:{}; 成交单编号:{}", pTrade.getOrderSysID(), pTrade.getExchangeID(), pTrade.getTraderID(), pTrade.getTradeID());
        
        String uuid = ctpRequestManager.lookupUUID(Integer.parseInt(pTrade.getOrderRef()));
        kafkaProducer.sendReturnTrade(uuid, pTrade);
    }

    ////////////////////////////////////////////////////////////////////////////
    //                              剩余暂不需要实现                           //
    ////////////////////////////////////////////////////////////////////////////
}
