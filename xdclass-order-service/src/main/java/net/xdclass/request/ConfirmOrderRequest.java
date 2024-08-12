package net.xdclass.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@ApiModel("订单项")
@Data
public class ConfirmOrderRequest {
    /**
     * 购物车优惠券，集满减券
     * 注意：如果传空或者小于0， 则不用优惠券
     */
    @JsonProperty("coupon_record_id")
    private Long couponRecordId;
    /**
     * 最终的购物车中的商品列表
     * 传递id，购买数量从购物车中读取
     */
    @JsonProperty("product_id_list")
    private List<Long> productIdList;
    /**
     * 支付方式
     */
    @JsonProperty("pay_type")
    private String payType;
    /**
     * 终端类型
     */
    @JsonProperty("client_type")
    private String clientType;

    /**
     * 收货地址id
     */
    @JsonProperty("address_id")
    private long addressId;
    /**
     * 总价格，前端传递，后端验证
     */
    @JsonProperty("total_price")
    private BigDecimal totalPrice;
    /**
     * 实际支付价格
     */
    @JsonProperty("real_pay_price")
    private BigDecimal realPayPrice;


}
