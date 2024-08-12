package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.RabbitMQConfig;
import net.xdclass.enums.*;
import net.xdclass.exception.BizException;
import net.xdclass.feign.ProductOrderFeignSerivce;
import net.xdclass.mapper.ProductMapper;
import net.xdclass.mapper.ProductTaskMapper;
import net.xdclass.model.ProductDO;
import net.xdclass.model.ProductMessage;
import net.xdclass.model.ProductTaskDO;
import net.xdclass.request.LockProductRequest;
import net.xdclass.request.OrderItemRequest;
import net.xdclass.service.ProductService;
import net.xdclass.util.JsonData;
import net.xdclass.vo.ProductVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitMQConfig rabbitMQConfig;
    @Autowired
    private ProductOrderFeignSerivce productOrderFeignSerivce;

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
                log.info("商品库存锁定-插入某商品product_task成功：{}",productTaskDO);
//                发送MQ延迟消息，介绍商品库存
                ProductMessage productMessage = new ProductMessage();
                productMessage.setTaskId(productTaskDO.getId());
                productMessage.setOutTradeNo(outTradeNo);

                rabbitTemplate.convertAndSend(rabbitMQConfig.getEventExchange(),rabbitMQConfig.getStockReleaseDelayRoutingKey(), productMessage);
                log.info("商品库存锁定信息发送延迟消息成功：{}",productMessage);

            }
        }

        return JsonData.buildSuccess();
    }

    /**
     * 释放商品库存
     * 1）查询task工作单是否存在
     * 2）如果存在，查询订单状态
     * @param productMessage
     * @return
     */
    @Override
    public boolean releaseProductStock(ProductMessage productMessage) {
        ProductTaskDO productTaskDO = productTaskMapper.selectOne(new QueryWrapper<ProductTaskDO>().eq("id",productMessage.getTaskId()));
        if (productTaskDO == null) {
            log.warn("工作单不存在，消息为：{}",productMessage);
        }
//        只有lock状态才进行处理
        if (productTaskDO.getLockState().equalsIgnoreCase(StockTaskStateEnum.LOCK.name())) {
//            查询该订单状态
            JsonData jsonData = productOrderFeignSerivce.queryProductOrderState(productMessage.getOutTradeNo());
            if (jsonData.getCode()==0) {
//                正常响应，判断订单的状态
                String state = jsonData.getData().toString();
                if (state.equalsIgnoreCase(ProductOrderStateEnum.NEW.name())) {
//                    状态为NEW新建状态， 则返回给消息队列， 继续重试
                    log.warn("订单状态为NEW, 返回给消息队列，重新投递：{}",productMessage);
                    return false;
                }
//                如果订单状态为已支付，将task工作单中的状态更改为finish
                if (state.equalsIgnoreCase(ProductOrderStateEnum.PAY.name())) {
                    productTaskDO.setLockState(StockTaskStateEnum.FINISH.name());
//                    进行更新
                    productTaskMapper.update(productTaskDO,new QueryWrapper<ProductTaskDO>().eq("id",productMessage.getTaskId()));
                    log.info("订单已经完成支付，修改库存锁定工作单为FINISH状态");
                    return true;
                }
            }
//            订单不存在，或者订单被取消，确认消息，修改task的状态为CANCEL， 恢复优惠券使用记录为NEW，而不是USED
            log.warn("订单不存在，或者订单被取消，确认消息，修改task的状态为CANCEL， 恢复优惠券使用记录为NEW，而不是USED, message:{}",productMessage);
            productTaskDO.setLockState(StockTaskStateEnum.CANCEL.name());
//            修改task的状态为CANCEL
            productTaskMapper.update(productTaskDO,new QueryWrapper<ProductTaskDO>().eq("id",productMessage.getTaskId()));
//            恢复商品库存，即锁定库存的值减去当前购买的值
            productMapper.unlockProductStock(productTaskDO.getProductId(),productTaskDO.getBuyNum());
            return true;
        }else {
            log.warn("工作单不是LOCK，state={},消息体={}",productTaskDO.getLockState(),productMessage);
            return true;
        }

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
