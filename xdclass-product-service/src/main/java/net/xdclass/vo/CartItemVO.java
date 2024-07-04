package net.xdclass.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车的商品项
 */
@Data
public class CartItemVO {
    /**
     * 商品id
     */
    @JsonProperty("product_id")
    private Long productId;
    /**
     * 购买数量
     */
    @JsonProperty("buy_num")
    private Integer buyNum;
    /**
     * 商品标题
     */
    @JsonProperty("product_title")
    private String prodctTitle;
    /**
     * 商品图片
     */
    @JsonProperty("product_img")
    private String productImg;
    /**
     * 商品价格
     */
    @JsonProperty("product_price")
    /**
     * 商品单价
     */
    private BigDecimal productPrice;
    /**
     * 商品总价
     */
    @JsonProperty("product_total_price")
    private BigDecimal productTotalPrice;

    /**
     * 商品单价 * 购买数量
     * @return
     */
    public BigDecimal getProductTotalPrice() {
        return this.productPrice.multiply(new BigDecimal(this.buyNum));
    }
}
