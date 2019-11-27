package com.nodeunify.jupiter.trader.ctp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import ctp.thosttraderapi.CThostFtdcInstrumentField;
import ctp.thosttraderapi.CThostFtdcQryInstrumentField;
import ctp.thosttraderapi.CThostFtdcReqAuthenticateField;
import ctp.thosttraderapi.CThostFtdcReqUserLoginField;
import ctp.thosttraderapi.CThostFtdcRspAuthenticateField;
import ctp.thosttraderapi.CThostFtdcRspInfoField;
import ctp.thosttraderapi.CThostFtdcRspUserLoginField;
import ctp.thosttraderapi.CThostFtdcTraderApi;
import ctp.thosttraderapi.CThostFtdcTraderSpi;
import ctp.thosttraderapi.THOST_TE_RESUME_TYPE;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CTPTrader {
    private CThostFtdcTraderApi traderApi;
    // Prod
    // private final static String ctp1_TradeAddress = "tcp://180.166.132.67:41205";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Value("#{'tcp://' + '${app.ctp.ip}' + ':' + '${app.ctp.port}'}")
    private String ctpTradeAddress;
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
    @Value("${spring.kafka.topic.instrument}")
    private String topic;

    static{
		System.loadLibrary("thosttraderapi_se");
		System.loadLibrary("thosttraderapi_wrap");
    }
    
    @PostConstruct
    public void postConstruct() {
        traderApi = CThostFtdcTraderApi.CreateFtdcTraderApi("trade");
        TraderSpiImpl traderSpi = new TraderSpiImpl();
        traderApi.RegisterSpi(traderSpi);
		traderApi.RegisterFront(ctpTradeAddress);
		traderApi.SubscribePublicTopic(THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
		traderApi.SubscribePrivateTopic(THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
        traderApi.Init();
        traderApi.Join();
    }
    
    @PreDestroy
    public void preDestroy() {
        traderApi.Release();
    }

    // TODO: to be used for listening signal in the future
    // @KafkaListener(topics = "test")
    // public void listenSignal(String payload) {

    // }

    class TraderSpiImpl extends CThostFtdcTraderSpi {
        final static String m_TradingDay = "20181122";
        final static String m_CurrencyId = "CNY";

        TraderSpiImpl() { }
        
        @Override
        public void OnFrontConnected() {
            log.debug("On Front Connected");
            CThostFtdcReqAuthenticateField field = new CThostFtdcReqAuthenticateField();
            field.setBrokerID(brokerId);
            field.setUserID(userId);
            field.setAppID(appId);
            field.setAuthCode(authCode);
            traderApi.ReqAuthenticate(field, 0);
            System.out.println("Send ReqAuthenticate ok");
        }

        @Override
        public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) 
        {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0) {
                System.out.printf("Login ErrorID[%d] ErrMsg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
                return;
            }
            System.out.println("OnRspAuthenticate success!!!");
            CThostFtdcReqUserLoginField field = new CThostFtdcReqUserLoginField();
            field.setBrokerID(brokerId);
            field.setUserID(userId);
            field.setPassword(password);
            traderApi.ReqUserLogin(field,0);
            System.out.println("Send login ok");
        }

        @Override
        public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
        {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0) {
                System.out.printf("Login ErrorID[%d] ErrMsg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
                return;
            }
            log.debug("Login success!!!");
            // CThostFtdcQryTradingAccountField qryTradingAccount = new CThostFtdcQryTradingAccountField();
            // qryTradingAccount.setBrokerID(m_BrokerId);
            // qryTradingAccount.setCurrencyID(m_CurrencyId);;
            // qryTradingAccount.setInvestorID(m_InvestorId);
            // //m_traderapi.ReqQryTradingAccount(qryTradingAccount, 1);
            
            // CThostFtdcQrySettlementInfoField qrysettlement = new CThostFtdcQrySettlementInfoField();
            // qrysettlement.setBrokerID(m_BrokerId);
            // qrysettlement.setInvestorID(m_InvestorId);
            // qrysettlement.setTradingDay(m_TradingDay);
            // qrysettlement.setAccountID(m_AccountId);
            // qrysettlement.setCurrencyID(m_CurrencyId);
            // //m_traderapi.ReqQrySettlementInfo(qrysettlement, 2);
            
            CThostFtdcQryInstrumentField qryInstr = new CThostFtdcQryInstrumentField();
            traderApi.ReqQryInstrument(qryInstr, 1);
            log.debug("Query success!!!");
        }

        @Override
        public void OnRspQryInstrument(CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
        {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0) {
                System.out.printf("OnRspQryInstrument ErrorID[%d] ErrMsg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
                return;
            }
            if (pInstrument != null) {
                System.out.printf("%s\n",pInstrument.getInstrumentID());
                kafkaTemplate.send(new ProducerRecord<String, String>(topic, pInstrument.getInstrumentID()));
            } else {
                System.out.printf("NULL obj\n");
            }
        }
    }
}
