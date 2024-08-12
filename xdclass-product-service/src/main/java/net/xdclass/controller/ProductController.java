package net.xdclass.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.xdclass.request.LockProductRequest;
import net.xdclass.service.ProductService;
import net.xdclass.util.JsonData;
import net.xdclass.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author ygk
 * @since 2024-06-30
 */
@Api(tags = "商品模块")
@RestController
@RequestMapping("/api/product/v1")
public class ProductController {
    @Autowired
    private ProductService productService;

    /**
     * 商品首页分页列表
     *
     * @return
     */
    @ApiOperation("商品首页分页列表")
    @GetMapping("page_product")
    public JsonData pageCouponList(@ApiParam(value = "当前页") @RequestParam(value = "page", defaultValue = "1") int page,
                                   @ApiParam(value = "每页显示多少条") @RequestParam(value = "size", defaultValue = "10") int size) {
        Map<String, Object> pageMap = productService.pageProductActivity(page, size);
        return JsonData.buildSuccess(pageMap);
    }

    @ApiOperation("根据id查看商品详情")
    @GetMapping("detail/{product_id}")
    public JsonData fidDetailById(@ApiParam(value = "商品Id", required = true) @PathVariable("product_id") long productId) {
        ProductVO productVO = productService.findDetailById(productId);
        return JsonData.buildSuccess(productVO);
    }

    @ApiOperation("商品库存锁定")
    @PostMapping("lock_products")
    public JsonData lockProducts(@ApiParam("商品库存锁定") @RequestBody LockProductRequest lockProductRequest){
        JsonData jsonData = productService.lockProductStock(lockProductRequest);
        return jsonData;
    }

}

