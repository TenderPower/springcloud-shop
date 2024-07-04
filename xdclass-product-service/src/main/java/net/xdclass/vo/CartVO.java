package net.xdclass.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartVO {
    /**
     * 购物车商品列表
     */
    @JsonProperty("cart_items")
    private List<CartItemVO> cartItems;
    /**
     * 购物车商品总数
     */
    @JsonProperty("total_count")
    private Integer totalCount;
    /**
     * 购物车总价格
     */
    @JsonProperty("total_price")
    private BigDecimal totalPrice;
    /**
     * 购物车实际价格
     */
    @JsonProperty("real_price")
    private BigDecimal realPrice;

    /**
     * 购物车总件数
     * @return
     */
    public Integer getTotalCount() {
        Integer totalCount = 0;
        if (this.cartItems != null) for (CartItemVO cartItemVO : cartItems) {
            totalCount += cartItemVO.getBuyNum();
        }
        return totalCount;
    }

    /**
     * 购物车总价格
     * @return
     */
    public BigDecimal getTotalPrice() {
        BigDecimal totalPrice = new BigDecimal(0);
        if (this.cartItems != null) {
            for (CartItemVO cartItemVO : cartItems){
                totalPrice = totalPrice.add(cartItemVO.getProductTotalPrice());
            }
        }
        return totalPrice;
    }

    /**
     * 购物车实际价格
     * @return
     */
    public BigDecimal getRealPrice() {
        BigDecimal totalPrice = new BigDecimal(0);
        if (this.cartItems != null) {
            for (CartItemVO cartItemVO : cartItems){
                totalPrice = totalPrice.add(cartItemVO.getProductTotalPrice());
            }
        }
        return totalPrice;
    }
}
