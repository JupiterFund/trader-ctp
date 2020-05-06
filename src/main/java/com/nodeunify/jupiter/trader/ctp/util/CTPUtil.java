package com.nodeunify.jupiter.trader.ctp.util;

public class CTPUtil {

    //客户端与交易后台通信连接断开原因
    public static final int BYWLAN = -1;//因网络原因发送失败
    public static final int REQNUMMAX = -2;//未处理请求队列总数量超限
    public static final int MISREQMAX = -3;//每秒发送请求数量超限

    public static final int NETREADFAIL = 0x1001;//网络读失败
    public static final int NETWRITEFAIL = 0x1002;//网络写失败
    public static final int HEARTOUT = 0x2001;//接收心跳超时
    public static final int HEARTFAIL = 0x2002;//发送心跳失败
    public static final int ERRORMSG = 0x2003;//收到错误报文

    public static int MAX_INT_VALUE = 2000000000;//请求号值阈
    public static int ORDER_REF_MIN_VALUE = 2000000001;//报单参照号最低阈值

    //CTP交易所定义和铭创交易所定义
    public static final String YES = "YES"; //收否勾选
    public static final String CTPDCE = "DCE"; //CTP大商所
    public static final String MCDCE = "XDCE"; //MC大商所

    public static final String CTPCFFEX = "CFFEX";//CTP中金所
    public static final String MCCFFEX = "CCFX";//MC中金所

    public static final String CTPSHFE = "SHFE";//CTP上期所
    public static final String MCSHFE = "XSGF";//MC上期所

    public static final String CTPCZCE = "CZCE";//CTP郑商所
    public static final String MCCZCE = "XZCE";//MC郑商所

    public static final String CTPXSGE = "INE";//CTP能源所
    public static final String MCXSGE = "XSGE";//MC能源所

    //期权合约产品代号
    public static final char OPTION_2 = '2';
    public static final char OPTION_6 = '6';

    //期货合约产品代号
    public static final char FUTURE_1 = '1';
    public static final char FUTURE_3 = '3';
    public static final char FUTURE_4 = '4';
    public static final char FUTURE_5 = '5';

    //保留消息ID
    public static final int REQUEST_ID_INIT = -1;
    public static final int REQUEST_ID_RELEASE = -2;

    //行情存储阀值
    public static final int RESPONNUM = 111;//每次响应111条,响应不足111条代表结束
    private static final float MAXFLOATVALUE = 1e11f;//异常值阀

    /**
     * 与前置机断开原因
     * @param reason
     * @return
     */
    public static String getReasonMsg(int reason) {
        String returnMsg;
        switch (reason) {
        case CTPUtil.BYWLAN:
            returnMsg = "因网络原因发送失败.";
            break;
        case CTPUtil.REQNUMMAX:
            returnMsg = "未处理请求队列总数量超限.";
            break;
        case CTPUtil.MISREQMAX:
            returnMsg = "每秒发送请求数量超限.";
            break;
        default:
            returnMsg = "未知原因代码: " + reason;
            break;
        }
        return returnMsg;
    }

    public static String getReasonTraderMsg(int nReason) {
        String returnMsg;
        switch (nReason) {
        case CTPUtil.NETREADFAIL:
            returnMsg = "网络读失败.";
            break;
        case CTPUtil.NETWRITEFAIL:
            returnMsg = "网络写失败.";
            break;
        case CTPUtil.HEARTOUT:
            returnMsg = "接收心跳超时.";
            break;
        case CTPUtil.HEARTFAIL:
            returnMsg = "发送心跳失败.";
            break;
        case CTPUtil.ERRORMSG:
            returnMsg = "收到错误报文.";
            break;
        default:
            returnMsg = "未知原因代码: " + nReason;
            break;
        }
        return returnMsg;
    }
}
