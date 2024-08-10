package net.xdclass.service;

import net.xdclass.model.ProductOrderDO;
import com.baomidou.mybatisplus.extension.service.IService;
import net.xdclass.request.ConfirmOrderRequest;
import net.xdclass.util.JsonData;

import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ygk
 * @since 2024-07-04
 */
public interface ProductOrderService {

    /**
     * 确认订单
     * @param confirmOrderRequest
     * @param response
     */
    JsonData confirmOrder(ConfirmOrderRequest confirmOrderRequest, HttpServletResponse response);

    /**
     * 查询订单状态
     * @param outTradeNo
     * @return
     */
    String queryProductOrderState(String outTradeNo);
}
