package net.xdclass.service;

import net.xdclass.model.ProductMessage;
import net.xdclass.request.LockProductRequest;
import net.xdclass.util.JsonData;
import net.xdclass.vo.ProductVO;

import java.util.List;
import java.util.Map;

public interface ProductService {
    /**
     * 商品首页分页列表接口
     * @param page
     * @param size
     * @return
     */
    Map<String, Object> pageProductActivity(int page, int size);

    /**
     * 根据商品id查询商品详情
     * @param productId
     * @return
     */
    ProductVO findDetailById(long productId);

    /**
     * 根据id批量查询商品详情
     * @param productIdList
     * @return
     */
    List<ProductVO> findDetailByIds(List<Long> productIdList);

    /**
     * 锁定商品库存
     * @param lockProductRequest
     * @return
     */
    JsonData lockProductStock(LockProductRequest lockProductRequest);

    /**
     * 释放商品库存
     * @param productMessage
     * @return
     */
    boolean releaseProductStock(ProductMessage productMessage);
}
