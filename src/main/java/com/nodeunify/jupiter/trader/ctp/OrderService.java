package com.nodeunify.jupiter.trader.ctp;

import com.nodeunify.jupiter.trader.ctp.impl.CTPTraderApi;
import com.nodeunify.jupiter.trader.ctp.v1.Order;
import com.nodeunify.jupiter.trader.ctp.v1.OrderAction;
import com.nodeunify.jupiter.trader.ctp.v1.OrderServiceGrpc;
import com.nodeunify.jupiter.trader.ctp.v1.OrderStreamRequest;
import com.nodeunify.jupiter.trader.ctp.v1.OrderStreamResponse;

import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GRpcService
public class OrderService extends OrderServiceGrpc.OrderServiceImplBase {

    @Autowired
    private CTPTraderApi traderApi;

    @Override
    public void orderInsert(Order request, StreamObserver<Order> responseObserver) {
        log.debug("orderInsert: {}", request);
    }

    @Override
    public void orderAction(OrderAction request, StreamObserver<Order> responseObserver) {
        // TODO Auto-generated method stub
        super.orderAction(request, responseObserver);
    }

    @Override
    public void parkedOrderInsert(Order request, StreamObserver<Order> responseObserver) {
        // TODO Auto-generated method stub
        super.parkedOrderInsert(request, responseObserver);
    }

    @Override
    public void parkedOrderAction(OrderAction request, StreamObserver<Order> responseObserver) {
        // TODO Auto-generated method stub
        super.parkedOrderAction(request, responseObserver);
    }

    @Override
    public void removeParkedOrder(Order request, StreamObserver<Order> responseObserver) {
        // TODO Auto-generated method stub
        super.removeParkedOrder(request, responseObserver);
    }

    @Override
    public void removeParkedOrderAction(OrderAction request, StreamObserver<Order> responseObserver) {
        // TODO Auto-generated method stub
        super.removeParkedOrderAction(request, responseObserver);
    }

    @Override
    public StreamObserver<OrderStreamRequest> orderStream(StreamObserver<OrderStreamResponse> responseObserver) {
        // TODO Auto-generated method stub
        return super.orderStream(responseObserver);
    }
    
}
