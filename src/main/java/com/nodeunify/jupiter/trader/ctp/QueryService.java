package com.nodeunify.jupiter.trader.ctp;

import com.nodeunify.jupiter.commons.mapper.DatastreamMapper;
import com.nodeunify.jupiter.commons.mapper.TraderCTPMapper;
import com.nodeunify.jupiter.datastream.v1.FutureData;
import com.nodeunify.jupiter.trader.ctp.impl.CTPTraderApi;
import com.nodeunify.jupiter.trader.ctp.v1.Instrument;
import com.nodeunify.jupiter.trader.ctp.v1.InvestorPosition;
import com.nodeunify.jupiter.trader.ctp.v1.InvestorPositionDetail;
import com.nodeunify.jupiter.trader.ctp.v1.QueryDepthMarketDataField;
import com.nodeunify.jupiter.trader.ctp.v1.QueryInstrumentField;
import com.nodeunify.jupiter.trader.ctp.v1.QueryInvestorPositionDetailField;
import com.nodeunify.jupiter.trader.ctp.v1.QueryInvestorPositionField;
import com.nodeunify.jupiter.trader.ctp.v1.QueryServiceGrpc;
import com.nodeunify.jupiter.trader.ctp.v1.QueryTradingAccountField;
import com.nodeunify.jupiter.trader.ctp.v1.TradingAccount;

import org.apache.logging.log4j.util.Strings;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import ctp.thosttraderapi.CThostFtdcDepthMarketDataField;
import ctp.thosttraderapi.CThostFtdcQryDepthMarketDataField;
import ctp.thosttraderapi.CThostFtdcQryInstrumentField;
import ctp.thosttraderapi.CThostFtdcQryInvestorPositionDetailField;
import ctp.thosttraderapi.CThostFtdcQryInvestorPositionField;
import ctp.thosttraderapi.CThostFtdcQryTradingAccountField;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GRpcService
public class QueryService extends QueryServiceGrpc.QueryServiceImplBase {

    @Autowired
    private CTPTraderApi traderApi;

    @Override
    public void queryInstrument(QueryInstrumentField request, StreamObserver<Instrument> responseObserver) {
        // FIXME: 目前只支持单个合约的查询. 若想查询全部合约，请使用Kafka接口查询.
        String instrumentID = request.getInstrumentID();
        if (Strings.isEmpty(instrumentID)) {
            Error error = new Error("无效合约号");
            log.error("期货合约号不能为空。当前仅支持单个合约的查询.", error);
            responseObserver.onError(error);
            return;
        }
        log.debug("[queryInstrument] 查询单个期货合约. InstrumentID:{}", instrumentID);
        CThostFtdcQryInstrumentField qryInstrumentField = TraderCTPMapper.MAPPER.map(request);
        traderApi.reqQryInstrument(qryInstrumentField).thenAccept(instrumentField -> {
            Instrument instrument = TraderCTPMapper.MAPPER.map(instrumentField);
            responseObserver.onNext(instrument);
            responseObserver.onCompleted();
        }).exceptionally(exception -> {
            log.warn("[queryInstrument] 期货合约查询异常", exception);
            responseObserver.onError(exception);
            return null;
        });
    }

    @Override
    public void queryInvestorPosition(QueryInvestorPositionField request,
            StreamObserver<InvestorPosition> responseObserver) {
        log.debug("[queryInvestorPosition] 查询持仓. QueryInvestorPositionField:{}", request);
        // TODO: more request check
        CThostFtdcQryInvestorPositionField qryInvestorPositionField = TraderCTPMapper.MAPPER.map(request);
        traderApi.reqQryInvestorPosition(qryInvestorPositionField).thenAccept(investorPositionFieldList -> {
            investorPositionFieldList.forEach(investorPositionField -> {
                InvestorPosition investorPosition = TraderCTPMapper.MAPPER.map(investorPositionField);
                log.debug("investorPosition: {}", investorPosition);
                responseObserver.onNext(investorPosition);
            });
            responseObserver.onCompleted();
        }).exceptionally(exception -> {
            log.warn("[queryInvestorPosition] 持仓查询异常", exception);
            responseObserver.onError(exception);
            return null;
        });
    }

    @Override
    public void queryInvestorPositionDetail(QueryInvestorPositionDetailField request,
            StreamObserver<InvestorPositionDetail> responseObserver) {
        log.debug("[queryInvestorPositionDetail] 查询持仓明细. QueryInvestorPositionDetailField:{}", request);
        CThostFtdcQryInvestorPositionDetailField qryInvestorPositionDetailField = TraderCTPMapper.MAPPER.map(request);
        traderApi.reqQryInvestorPositionDetail(qryInvestorPositionDetailField).thenAccept(investorPositionDetailFieldList -> {
            investorPositionDetailFieldList.forEach(investorPositionDetailField -> {
                InvestorPositionDetail investorPositionDetail = TraderCTPMapper.MAPPER.map(investorPositionDetailField);
                log.debug("investorPositionDetail: {}", investorPositionDetail);
                responseObserver.onNext(investorPositionDetail);
            });
            responseObserver.onCompleted();
        }).exceptionally(exception -> {
            log.warn("[queryInvestorPositionDetail] 持仓明细查询异常", exception);
            responseObserver.onError(exception);
            return null;
        });
    }

    @Override
    public void queryTradingAccount(QueryTradingAccountField request, StreamObserver<TradingAccount> responseObserver) {
        log.debug("[queryTradingAccount] 查询交易账户.");
        // TODO: more request check
        CThostFtdcQryTradingAccountField qryTradingAccountField = TraderCTPMapper.MAPPER.map(request);
        traderApi.reqQryTradingAccount(qryTradingAccountField).thenAccept(tradingAccountField -> {
            TradingAccount tradingAccount = TraderCTPMapper.MAPPER.map(tradingAccountField);
            responseObserver.onNext(tradingAccount);
            responseObserver.onCompleted();
        }).exceptionally(exception -> {
            log.warn("[queryTradingAccount] 交易账户查询异常", exception);
            responseObserver.onError(exception);
            return null;
        });
    }

    @Override
    public void queryDepthMarketData(QueryDepthMarketDataField request, StreamObserver<FutureData> responseObserver) {
        log.debug("[queryDepthMarketData] 查询行情数据.");
        // TODO: more request check
        CThostFtdcQryDepthMarketDataField qryDepthMarketDataField = TraderCTPMapper.MAPPER.map(request);
        traderApi.reqQryDepthMarketData(qryDepthMarketDataField).thenAccept(depthMarketDataField -> {
            sanitize(depthMarketDataField);
            FutureData futureData = DatastreamMapper.MAPPER.map(depthMarketDataField);
            log.debug("futureData: {}", futureData);
            responseObserver.onNext(futureData);
            responseObserver.onCompleted();
        }).exceptionally(exception -> {
            log.warn("[queryDepthMarketData] 行情数据查询异常", exception);
            responseObserver.onError(exception);
            return null;
        });
    }

    /**
     * 同Postman中一样的数据源问题。临时解决办法一致。 参考:
     * https://github.com/JupiterFund/postman/blob/master/src/main/java/com/nodeunify/jupiter/postman/source/CTPSource.java#L209
     * 
     * @param pDepthMarketData
     */
    private void sanitize(CThostFtdcDepthMarketDataField pDepthMarketData) {
        pDepthMarketData.setCurrDelta(0.0);
        pDepthMarketData.setPreDelta(0.0);
    }
}
