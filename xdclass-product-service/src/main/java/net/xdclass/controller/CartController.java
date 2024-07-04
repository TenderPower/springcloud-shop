package net.xdclass.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.xdclass.request.CartItemRequest;
import net.xdclass.service.CartService;
import net.xdclass.util.JsonData;
import net.xdclass.vo.CartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "购物车模块")
@RestController
@RequestMapping("/api/cart/v1")
public class CartController {
    @Autowired
    private CartService cartService;

    @ApiOperation("将商品添加到购物车中")
    @PostMapping("add")
    public JsonData addToCart(@ApiParam(value = "购物的商品") @RequestBody CartItemRequest cartItemRequest) {
        cartService.addCartItem(cartItemRequest);
        return JsonData.buildSuccess("商品添加成功");
    }

    @ApiOperation("清空购物车")
    @DeleteMapping("clear")
    public JsonData clearCart() {
        cartService.clearCart();
        return JsonData.buildSuccess("购物车清空成功");
    }

    @ApiOperation("删除购物车中的商品")
    @DeleteMapping("del/{product_id}")
    public JsonData delCartItem(@ApiParam(value = "商品id", required = true) @RequestParam(value = "product_id") long productId) {
        cartService.delCartItem(productId);
        return JsonData.buildSuccess("删除商品成功");
    }

    @ApiOperation("查看购物车")
    @GetMapping("mycart")
    public JsonData findCart() {
        CartVO cartVO = cartService.getMyCart();
        return JsonData.buildSuccess(cartVO);
    }

    @ApiOperation("更新购物车的商品数量")
    @GetMapping("update")
    public JsonData updateCartItemNum(@ApiParam(value = "购物车商品", required = true) @RequestBody CartItemRequest cartItemRequest){
        cartService.updateCartItemNum(cartItemRequest);
        return JsonData.buildSuccess("更新商品数量");
    }

}

