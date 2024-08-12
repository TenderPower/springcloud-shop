package net.xdclass.service;

import net.xdclass.request.CartItemRequest;
import net.xdclass.util.JsonData;
import net.xdclass.vo.CartItemVO;
import net.xdclass.vo.CartVO;

import java.util.List;

public interface CartService {
    /**
     * 将商品添加到购物车中
     */
    void addCartItem(CartItemRequest cartItemRequest);

    /**
     * 清空购物车
     */
    void clearCart();

    /**
     * 查看我的购物车
     * @return
     */
    CartVO getMyCart();

    /**
     * 删除购物车商品
     * @param productId
     */
    void delCartItem(long productId);

    /**
     * 更新购物车商品数量
     * @param cartItemRequest
     */
    void updateCartItemNum(CartItemRequest cartItemRequest);

    /**
     * 获取用户在购物车中所要确定下单的商品列表信息
     * @param productIdList
     * @return
     */
    List<CartItemVO> confirmOrderCartItems(List<Long> productIdList);
}
