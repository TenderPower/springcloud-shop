package net.xdclass.service.impl;

import com.alibaba.fastjson2.JSON;
import net.xdclass.constant.CacheKey;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.exception.BizException;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.model.LoginUser;
import net.xdclass.request.CartItemRequest;
import net.xdclass.service.CartService;
import net.xdclass.service.ProductService;
import net.xdclass.vo.CartItemVO;
import net.xdclass.vo.CartVO;
import net.xdclass.vo.ProductVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import springfox.documentation.spring.web.json.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductService productService;


    @Override
    public void addCartItem(CartItemRequest cartItemRequest) {
        Long productId = cartItemRequest.getProductId();
        int buyNum = cartItemRequest.getBuyNum();
//        从redis中获取用户对应的购物车
        BoundHashOperations<String, Object, Object> myCartOps = getMyCartOps();
        String result = "";
//        cachedValue 是json格式的商品信息
        Object cachedValue = myCartOps.get(productId);
        if (cachedValue != null) {
            result = (String) cachedValue;
        }
        if (StringUtils.isBlank(result)) {
//            购物车不存在这个商品，需要新加入
            CartItemVO cartItemVO = new CartItemVO();
//            从商品服务器中获取给商品
            ProductVO productVO = productService.findDetailById(productId);
            cartItemVO.setProductId(productId);
            cartItemVO.setBuyNum(buyNum);

            cartItemVO.setProdctTitle(productVO.getTitle());
            cartItemVO.setProductImg(productVO.getCoverImg());
            cartItemVO.setProductPrice(productVO.getPrice());

            myCartOps.put(productId, JSON.toJSONString(cartItemVO));
        } else {
//            否则，只更改对应商品的数量即可
            CartItemVO cartItemVO = JSON.parseObject(result, CartItemVO.class);
            cartItemVO.setBuyNum(cartItemVO.getBuyNum() + buyNum);
            myCartOps.put(productId, JSON.toJSONString(cartItemVO));
        }
    }

    /**
     * 清空购物车
     */
    @Override
    public void clearCart() {
        String cartKey = getCartKey();
        redisTemplate.delete(cartKey);
    }

    /**
     * 删除购物车中指定商品
     * @param productId
     */
    @Override
    public void delCartItem(long productId) {
        BoundHashOperations<String, Object, Object> myCart = getMyCartOps();
        myCart.delete(productId);
    }

    /**
     * 更新购物车中商品数量
     * @param cartItemRequest
     */
    @Override
    public void updateCartItemNum(CartItemRequest cartItemRequest) {
        Long productId = cartItemRequest.getProductId();
        int buyNum = cartItemRequest.getBuyNum();

        BoundHashOperations<String, Object, Object> myCart = getMyCartOps();
        Object cachedValue  = myCart.get(productId);
        if (cachedValue == null) {
            throw new BizException(BizCodeEnum.CART_FAIL);
        }
        String result = (String) cachedValue;
        CartItemVO cartItemVO = JSON.parseObject(result, CartItemVO.class);
        cartItemVO.setBuyNum(buyNum);

        myCart.put(productId, JSON.toJSONString(cartItemVO));


    }

    /**
     * 查询购物车
     *
     * @return
     */
    @Override
    public CartVO getMyCart() {
//        查询购物车中所有的商品列表(最新 or  不是最新)
        List<CartItemVO> cartItemVOList = getCartItems(false);

//        封装成CartVO
        CartVO cartVO = new CartVO();
        cartVO.setCartItems(cartItemVOList);


        return cartVO;
    }


    private List<CartItemVO> getCartItems(boolean latestPrice) {
//        从redis中获取用户对应的购物车
        BoundHashOperations<String, Object, Object> myCartOps = getMyCartOps();
        List<Object> items = myCartOps.values();
        List<CartItemVO> cartItemVOList = new ArrayList<>();

//        拼接商品id列表
        List<Long> productIdList = new ArrayList<>();
        for (Object item : items) {
            CartItemVO cartItemVO = JSON.parseObject((String) item, CartItemVO.class);
            cartItemVOList.add(cartItemVO);
            productIdList.add(cartItemVO.getProductId());

        }
//        查询最新的商品价格
        if (latestPrice) {
//            根据商品id列表，查询最新的商品价格
            setProductLatestPrice(cartItemVOList, productIdList);
        }
        return cartItemVOList;
    }

    /**
     * 设置商品最新价格
     *
     * @param cartItemVOList
     * @param productIdList
     */
    private void setProductLatestPrice(List<CartItemVO> cartItemVOList, List<Long> productIdList) {
        List<ProductVO> productVOList = productService.findDetailByIds(productIdList);

        /**
         * 使用Collectors.toMap()方法指定收集方式，传入两个函数式接口作为参数：
         * ProductVO::getId作为key的映射函数，用于从ProductVO对象中提取ID作为键。
         * Function.identity()作为value的映射函数，直接返回流中的每个元素本身作为值。
         */
//        最终得到一个Map<Long, ProductVO>，其中键为ProductVO对象的ID，值为对应的ProductVO对象。
        Map<Long, ProductVO> maps = productVOList.stream().collect(Collectors.toMap(ProductVO::getId, Function.identity()));

//        更新服务器中的商品详情(价格-标题-图片)
        cartItemVOList.forEach(cartItemVO -> {
            ProductVO productVO = maps.get(cartItemVO.getProductId());
            cartItemVO.setProductPrice(productVO.getPrice());
            cartItemVO.setProdctTitle(productVO.getTitle());
            cartItemVO.setProductImg(productVO.getCoverImg());
        });
    }

    /**
     * 抽取我Redis中购物车的Hash对象
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getMyCartOps() {
        return redisTemplate.boundHashOps(getCartKey());
    }

    /**
     * 获取在Redis中购物车的Key
     *
     * @return
     */
    private String getCartKey() {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        return String.format(CacheKey.CART_KEY, loginUser.getId());
    }
}
