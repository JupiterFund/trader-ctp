package com.nodeunify.jupiter.trader.ctp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
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

@Service
public class TraderService {
    private CThostFtdcTraderApi traderApi;

    // SimNow
    private final static String ctp1_TradeAddress = "tcp://180.168.146.187:10130";
    // Prod
    // private final static String ctp1_TradeAddress = "tcp://180.166.132.67:41205";

    static{
		System.loadLibrary("thosttraderapi_se");
		System.loadLibrary("thosttraderapi_wrap");
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @PostConstruct
    public void postConstruct() {
        traderApi = CThostFtdcTraderApi.CreateFtdcTraderApi("trade");
        TraderSpiImpl traderSpi = new TraderSpiImpl();
        traderApi.RegisterSpi(traderSpi);
		traderApi.RegisterFront(ctp1_TradeAddress);
		traderApi.SubscribePublicTopic(THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
		traderApi.SubscribePrivateTopic(THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
        traderApi.Init();
        traderApi.Join();
    }
    
    @PreDestroy
    public void preDestroy() {
    }

    @KafkaListener(topics = "test")
    public void listenSignal(String payload) {

    }

    class TraderSpiImpl extends CThostFtdcTraderSpi {
        // SimNow
        final static String m_BrokerId = "9999";
        final static String m_UserId = "012798";
        final static String m_PassWord = "123456"; 
        final static String m_InvestorId = "012798";
        final static String m_TradingDay = "20181122";
        final static String m_AccountId = "012798";
        final static String m_CurrencyId = "CNY";
        final static String m_AppId = "simnow_client_test";
        final static String m_AuthCode = "0000000000000000";
        // Prod
        // final static String m_BrokerId = "6000";
        // final static String m_UserId = "00303386";
        // final static String m_PassWord = "miyuan38"; 
        // final static String m_InvestorId = "00303386";
        // final static String m_TradingDay = "20181122";
        // final static String m_AccountId = "00303386";
        // final static String m_CurrencyId = "CNY";
        // final static String m_AppId = "client_juno_1.0.0";
        // final static String m_AuthCode = "AQJMBGYKDIHX8IZW";

        TraderSpiImpl() { }

        @Override
        public void OnFrontConnected(){
            System.out.println("On Front Connected");
            CThostFtdcReqAuthenticateField field = new CThostFtdcReqAuthenticateField();
            field.setBrokerID(m_BrokerId);
            field.setUserID(m_UserId);
            field.setAppID(m_AppId);
            field.setAuthCode(m_AuthCode);
            traderApi.ReqAuthenticate(field, 0);
            System.out.println("Send ReqAuthenticate ok");
        }

        @Override
        public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) 
        {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0)
            {
                System.out.printf("Login ErrorID[%d] ErrMsg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());

                return;
            }
            System.out.println("OnRspAuthenticate success!!!");
            CThostFtdcReqUserLoginField field = new CThostFtdcReqUserLoginField();
            field.setBrokerID(m_BrokerId);
            field.setUserID(m_UserId);
            field.setPassword(m_PassWord);
            traderApi.ReqUserLogin(field,0);
            System.out.println("Send login ok");
        }

        @Override
        public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
        {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0)
            {
                System.out.printf("Login ErrorID[%d] ErrMsg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());

                return;
            }
            System.out.println("Login success!!!");
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
            System.out.println("Query success!!!");
        }

        @Override
        public void OnRspQryInstrument(CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
        {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0)
            {
                System.out.printf("OnRspQryInstrument ErrorID[%d] ErrMsg[%s]\n", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
                return;
            }
            if (pInstrument != null)
            {
                System.out.printf("%s\n",pInstrument.getInstrumentID());
                // System.out.printf("%s\n",pInstrument.getCreateDate());
                // System.out.printf("%s\n",pInstrument.getOpenDate());
                // System.out.printf("%s\n",pInstrument.getExpireDate());
                // System.out.printf("%s\n",pInstrument.getDeliveryMonth());
                // System.out.printf("%s\n",pInstrument.getDeliveryYear());
                // System.out.printf("%s\n",pInstrument.getStartDelivDate());
                // System.out.printf("%s\n", pInstrument.getEndDelivDate());
                kafkaTemplate.send(new ProducerRecord<String, String>("test.ctp.instruments", pInstrument.getInstrumentID()));
            }
            else
            {
                System.out.printf("NULL obj\n");
            }
        }
    }
}
