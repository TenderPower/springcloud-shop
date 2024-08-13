package net.xdclass.service;

import net.xdclass.model.OrderMessage;
import net.xdclass.model.ProductOrderDO;
import com.baomidou.mybatisplus.extension.service.IService;
import net.xdclass.request.ConfirmOrderRequest;
import net.xdclass.util.JsonData;

import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ygk
 * @since 2024-07-04
 */
public interface ProductOrderService {

    /**
     * 确认订单
     *
     * @param confirmOrderRequest
     */
    JsonData confirmOrder(ConfirmOrderRequest confirmOrderRequest);

    /**
     * 查询订单状态
     *
     * @param outTradeNo
     * @return
     */
    String queryProductOrderState(String outTradeNo);

    /**
     * 关闭订单(确认订单是否支付成功，并关闭订单)
     *
     * @param orderMessage
     * @return
     */
    boolean closeProductOrder(OrderMessage orderMessage);
}
