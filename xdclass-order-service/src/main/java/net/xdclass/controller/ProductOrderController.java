package net.xdclass.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.ClientType;
import net.xdclass.enums.ProductOrderPayTypeEnum;
import net.xdclass.enums.ProductOrderTypeEnum;
import net.xdclass.request.ConfirmOrderRequest;
import net.xdclass.service.ProductOrderService;
import net.xdclass.util.JsonData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import springfox.documentation.spring.web.json.Json;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author ygk
 * @since 2024-07-04
 */
@Api("产品订单模块")
@RestController
@RequestMapping("/api/order/v1/")
@Slf4j
public class ProductOrderController {
    @Autowired
    private ProductOrderService productOrderService;

    /**
     * 查询订单支付状态
     * 此接口没有登录拦截，可以增加一个密钥进行RPC通信
     * @param outTradeNo
     * @return
     */
    @ApiOperation("查询订单状态")
    @GetMapping("query_state")
    JsonData queryProductOrderState(@ApiParam(value = "订单号",required = true) @RequestParam("out_trade_no") String outTradeNo){
        String state = productOrderService.queryProductOrderState(outTradeNo);
        return JsonData.buildSuccess(StringUtils.isBlank(state)?JsonData.buildResult(BizCodeEnum.ORDER_CONFIRM_NOT_EXIST):JsonData.buildSuccess(state));
    }
    @ApiOperation("确认订单")
    @PostMapping("confirm")
    public void confirmOrder(@ApiParam("订单对象") @RequestBody ConfirmOrderRequest confirmOrderRequest, HttpServletResponse response) {
        JsonData jsonData = productOrderService.confirmOrder(confirmOrderRequest);

        if (jsonData.getCode()==0) {
//            判断移动端 和 支付方式
            String client = confirmOrderRequest.getClientType();
            String payType = confirmOrderRequest.getPayType();
//            支付宝
            if(payType.equalsIgnoreCase(ProductOrderPayTypeEnum.ALIPAY.name())){
                log.info("创建支付宝订单成功：{}",confirmOrderRequest.toString());

                if (client.equalsIgnoreCase(ClientType.H5.name())) {
//                    将结果写入到HTTP响应中，以便客户端可以接收到该数据
                    wirteData(response, jsonData);
                } else if (client.equalsIgnoreCase(ClientType.APP.name())) {
//                    APP SDK 支付 TODO
                }

            }else if (payType.equalsIgnoreCase(ProductOrderPayTypeEnum.WECHAT.name())){
//                微信支付  TODO
            }
        }else {
            log.error("创建订单提交错误：{}",jsonData.toString());
        }
    }

    /**
     * 该函数用于将jsonData中的数据写入到HTTP响应中，以便客户端可以接收到该数据
     * @param response
     * @param jsonData
     */

    private void wirteData(HttpServletResponse response, JsonData jsonData) {
        try {
//           设置响应的内容类型为text/html;charset=UTF8，表示响应的内容是HTML页面，字符集为UTF-8
            response.setContentType("text/html;charset=UTF8");
//            调用response.getWriter()获取一个PrintWriter对象，用于向响应体中写入数据
            response.getWriter().write(jsonData.getData().toString());
//            调用flush()方法将缓冲区中的数据刷新到响应体中。
            response.getWriter().flush();
//            调用close()方法关闭输出流。
            response.getWriter().close();
        }catch (IOException e){
            log.error("数据写入Http响应头异常{}",e);
        }
    }
}

