package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.CouponCategoryEnum;
import net.xdclass.enums.CouponPublishEnum;
import net.xdclass.enums.StockTaskStateEnum;
import net.xdclass.exception.BizException;
import net.xdclass.mapper.ProductMapper;
import net.xdclass.mapper.ProductTaskMapper;
import net.xdclass.model.ProductDO;
import net.xdclass.model.ProductTaskDO;
import net.xdclass.request.LockProductRequest;
import net.xdclass.request.OrderItemRequest;
import net.xdclass.service.ProductService;
import net.xdclass.util.JsonData;
import net.xdclass.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductTaskMapper productTaskMapper;

    /**
     * 商品首页分页列表接口
     *
     * @param page
     * @param size
     * @return
     */
    @Override
    public Map<String, Object> pageProductActivity(int page, int size) {
        //        分页场景的使用：
//        1.new Page对象
        Page<ProductDO> pageInfo = new Page<>(page, size);
//        2.开始查询 并返回结果
        IPage<ProductDO> productDOIPage = productMapper.selectPage(pageInfo, null);
//        3.映射到Map中
        Map<String, Object> pageMap = new HashMap<>(3);
//        总条数
        pageMap.put("total_record", productDOIPage.getTotal());
//        总页数
        pageMap.put("total_page", productDOIPage.getPages());
        pageMap.put("current_data", productDOIPage.getRecords()
//                将Do 转为Vo 传给前端
                .stream()
                .map(obj -> beanProcess(obj)).collect(Collectors.toList()));

        return pageMap;
    }

    /**
     * 根据商品id查询商品详情
     * @param productId
     * @return
     */
    @Override
    public ProductVO findDetailById(long productId) {
        ProductDO productDO = productMapper.selectById(productId);
        return beanProcess(productDO);
    }

    /**
     * 根据商品id查询商品详情
     * @param productIdList
     * @return
     */
    @Override
    public List<ProductVO> findDetailByIds(List<Long> productIdList) {
        List<ProductDO> productDOList =productMapper.selectList(new QueryWrapper<ProductDO>().in("id", productIdList));
        return productDOList.stream().map(obj -> beanProcess(obj)).collect(Collectors.toList());
    }

    /**
     * 锁定商品库存
     * 1）遍历提交订单中的所有商品，锁定每个商品的数量
     * 2）每一次锁定，都要发送延迟消息
     * @param lockProductRequest
     * @return
     */
    @Override
    public JsonData lockProductStock(LockProductRequest lockProductRequest) {
        String outTradeNo = lockProductRequest.getOrderOutTradeNo();
        List<OrderItemRequest> itemList = lockProductRequest.getOrderItemList();

//        一行代码， 搞定提取对象中id并加入到集合里面
//        使用lamda表达式
        List<Long> productIdList = itemList.stream().map(OrderItemRequest::getProductId).collect(Collectors.toList());
        List<ProductVO> productVOList = this.findDetailByIds(productIdList);
        /**
         * 使用Collectors.toMap()方法指定收集方式，传入两个函数式接口作为参数：
         * ProductVO::getId作为key的映射函数，用于从ProductVO对象中提取ID作为键。
         * Function.identity()作为value的映射函数，直接返回流中的每个元素本身作为值。
         */
//        最终得到一个Map<Long, ProductVO>，其中键为ProductVO对象的ID，值为对应的ProductVO对象。
        Map<Long, ProductVO> maps = productVOList.stream().collect(Collectors.toMap(ProductVO::getId, Function.identity()));

        for (OrderItemRequest item : itemList) {
//            锁定商品记录
            int row = productMapper.lockProductStock(item.getProductId(), item.getBuyNum());
            if (row != 1) {
                throw new BizException(BizCodeEnum.ORDER_CONFIRM_LOCK_PRODUCT_FAIL);
            }else {
//                插入商品product_task
                ProductVO productVO = maps.get(item.getProductId());
                ProductTaskDO productTaskDO = new ProductTaskDO();
                productTaskDO.setBuyNum(item.getBuyNum());
                productTaskDO.setLockState(StockTaskStateEnum.LOCK.name());
                productTaskDO.setProductId(item.getProductId());
                productTaskDO.setProductName(productVO.getTitle());
                productTaskDO.setOutTradeNo(outTradeNo);
                productTaskMapper.insert(productTaskDO);
//                发送MQ延迟消息，介绍商品库存 TODO

            }
        }




        return JsonData.buildSuccess();
    }

    /**
     * 将DO对象 转为 VO对象
     *
     * @param productDO
     * @return
     */
    private ProductVO beanProcess(ProductDO productDO) {
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(productDO, productVO);
//        商品剩余的库存
        productVO.setStock(productDO.getStock()-productDO.getLockStock());
        return productVO;
    }
}
