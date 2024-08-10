package net.xdclass.enums;

/**
 * 订单的状态
 */
public enum ProductOrderStateEnum {

    /**
     * 未支付订单
     */
    NEW,


    /**
     * 已经支付订单
     */
    PAY,

    /**
     * 超时取消订单
     */
    CANCEL;

}