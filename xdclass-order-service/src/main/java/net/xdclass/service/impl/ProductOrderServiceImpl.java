package net.xdclass.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.RabbitMQConfig;
import net.xdclass.enums.*;
import net.xdclass.exception.BizException;
import net.xdclass.fegin.CouponFeignService;
import net.xdclass.fegin.ProductFeignService;
import net.xdclass.fegin.UserFeignService;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.ProductOrderItemMapper;
import net.xdclass.model.LoginUser;
import net.xdclass.model.OrderMessage;
import net.xdclass.model.ProductOrderDO;
import net.xdclass.mapper.ProductOrderMapper;
import net.xdclass.model.ProductOrderItemDO;
import net.xdclass.request.ConfirmOrderRequest;
import net.xdclass.request.LockCouponRecordRequest;
import net.xdclass.request.LockProductRequest;
import net.xdclass.request.OrderItemRequest;
import net.xdclass.service.ProductOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.xdclass.util.CommonUtil;
import net.xdclass.util.JsonData;
import net.xdclass.vo.CouponRecordVO;
import net.xdclass.vo.OrderItemVO;
import net.xdclass.vo.ProductOrderAddressVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.Null;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springfox.documentation.spring.web.json.Json;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ygk
 * @since 2024-07-04
 */
@Service
@Slf4j
public class ProductOrderServiceImpl implements ProductOrderService {
    @Autowired
    private ProductOrderMapper productOrderMapper;
    @Autowired
    private UserFeignService userFeignService;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private CouponFeignService couponFeignService;
    @Autowired
    private ProductOrderItemMapper productOrderItemMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    /**
     * 订单确认
     *
     * @param confirmOrderRequest
     */
    @Override
    public JsonData confirmOrder(ConfirmOrderRequest confirmOrderRequest) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();

//        随机生成一个订单号
        String orderOutTradeNo = CommonUtil.getStringNumRandom(32);

//        获取用户的地址信息
        ProductOrderAddressVO addressVO = this.getUserAddress(confirmOrderRequest.getAddressId());
        log.info("收货地址的信息：{}", addressVO);

//        获取用户在购物车中想要下单的商品
        List<Long> productIdList = confirmOrderRequest.getProductIdList();
        JsonData cartItemData = productFeignService.confirmOrderCartItems(productIdList);
        List<OrderItemVO> orderItemList = cartItemData.getData(new TypeReference<>() {
        });
        log.info("用户想要下单的商品：{}", orderItemList);
        if (orderItemList == null) {
//            商品不存在
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_CART_ITEM_NOT_EXIST);
        }
//        统计商品价格 - 远程调用获取优惠券- 当前购物车是否满足优惠券使用条件-验证价格
        checkPrice(orderItemList, confirmOrderRequest);
//        锁定操作 只是假装已经进行扣减库存 和消费了优惠券，当出现异常时，对应的微服务会回滚库存和优惠券
//        锁定优惠券(调用coupon微服务（向MQ发送）)
        lockCouponRecords(confirmOrderRequest, orderOutTradeNo);
//        锁定库存(调用product微服务（向MQ发送）
        lockProductStocks(orderItemList, orderOutTradeNo);
//        创建订单
        ProductOrderDO productOrderDO = saveProductOrder(orderOutTradeNo, addressVO, loginUser, confirmOrderRequest);
//        创建该订单对应的商品项
        saveProductOrderItems(orderOutTradeNo, productOrderDO.getId(), orderItemList);
//        发送延迟消息
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOutTradeNo(orderOutTradeNo);
        rabbitTemplate.convertAndSend(rabbitMQConfig.getEventExchange(),rabbitMQConfig.getOrderCloseDelayRoutingKey(),orderMessage);

//        创建支付 TODO


        return JsonData.buildSuccess();
    }

    /**
     * 创建该订单对应的商品项
     *
     * @param orderOutTradeNo
     * @param id
     * @param orderItemList
     */
    private void saveProductOrderItems(String orderOutTradeNo, Long id, List<OrderItemVO> orderItemList) {
        List<ProductOrderItemDO> list = orderItemList.stream().map(obj -> {
            ProductOrderItemDO itemDO = new ProductOrderItemDO();
            itemDO.setBuyNum(obj.getBuyNum());
            itemDO.setProductId(obj.getProductId());
            itemDO.setProductImg(obj.getProductImg());
            itemDO.setProductName(obj.getProdctTitle());

            itemDO.setOutTradeNo(orderOutTradeNo);
            itemDO.setCreateTime(new Date());

//        该商品的单价
            itemDO.setAmount(obj.getProductPrice());
//        该商品的总价
            itemDO.setTotalAmount(obj.getProductTotalPrice());
//        当前商品属于哪个订单
            itemDO.setProductOrderId(id);
            return itemDO;
        }).collect(Collectors.toList());

        productOrderItemMapper.insertBatch(list);
    }

    /**
     * 创建订单
     *
     * @param orderOutTradeNo
     * @param addressVO
     * @param loginUser
     * @param confirmOrderRequest
     */
    private ProductOrderDO saveProductOrder(String orderOutTradeNo, ProductOrderAddressVO addressVO, LoginUser loginUser, ConfirmOrderRequest confirmOrderRequest) {
        ProductOrderDO productOrderDO = new ProductOrderDO();

        productOrderDO.setUserId(loginUser.getId());
        productOrderDO.setHeadImg(loginUser.getHeadImg());
        productOrderDO.setNickname(loginUser.getName());

        productOrderDO.setOutTradeNo(orderOutTradeNo);
        productOrderDO.setCreateTime(new Date());
        productOrderDO.setDel(0);
        productOrderDO.setOrderType(ProductOrderTypeEnum.DAILY.name());

//        实际支付的价格
        productOrderDO.setPayAmount(confirmOrderRequest.getRealPayPrice());

//        总价， 未使用优惠券的价格
        productOrderDO.setTotalAmount(confirmOrderRequest.getTotalPrice());
        productOrderDO.setState(ProductOrderStateEnum.NEW.name());
//        从请求中获取支付的方式
        productOrderDO.setPayType(ProductOrderPayTypeEnum.valueOf(confirmOrderRequest.getPayType()).name());

        productOrderDO.setReceiverAddress(JSON.toJSONString(addressVO));

        productOrderMapper.insert(productOrderDO);
        return productOrderDO;
    }

    /**
     * 锁定库存
     *
     * @param orderItemList
     * @param orderOutTradeNo
     */
    private void lockProductStocks(List<OrderItemVO> orderItemList, String orderOutTradeNo) {
        List<OrderItemRequest> itemRequestList = orderItemList.stream().map(obj -> {
            OrderItemRequest orderItemRequest = new OrderItemRequest();
            orderItemRequest.setBuyNum(obj.getBuyNum());
            orderItemRequest.setProductId(obj.getProductId());
            return orderItemRequest;
        }).collect(Collectors.toList());

        LockProductRequest lockProductRequest = new LockProductRequest();
        lockProductRequest.setOrderItemList(itemRequestList);
        lockProductRequest.setOrderOutTradeNo(orderOutTradeNo);

//            RPC 发起锁定商品库存记录的请求（向product微服务发送）
        JsonData jsonData = productFeignService.lockProducts(lockProductRequest);
        if (jsonData.getCode() != 0) {
            log.error("锁定商品库存失败：{}", lockProductRequest);
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_LOCK_PRODUCT_FAIL);
        }
    }

    /**
     * 锁定优惠券
     *
     * @param confirmOrderRequest
     * @param orderOutTradeNo
     */
    private void lockCouponRecords(ConfirmOrderRequest confirmOrderRequest, String orderOutTradeNo) {
        List<Long> lockCouponRecordIds = new ArrayList<>();
        if (confirmOrderRequest.getCouponRecordId() > 0) {
            lockCouponRecordIds.add(confirmOrderRequest.getCouponRecordId());

            LockCouponRecordRequest lockCouponRecordRequest = new LockCouponRecordRequest();
            lockCouponRecordRequest.setLockCouponRecordIds(lockCouponRecordIds);
            lockCouponRecordRequest.setOrderOutTradeNo(orderOutTradeNo);

//            RPC 发起锁定优惠券记录的请求（向coupon微服务发送）
            JsonData jsonData = couponFeignService.lockCouponRecord(lockCouponRecordRequest);
            if (jsonData.getCode() != 0) {
                throw new BizException(BizCodeEnum.COUPON_RECORD_LOCK_FAIL);
            }
        }
    }

    /**
     * 验证使用优惠券 价格的合理性
     * 1）统计商品价格
     * 2）远程调用获取优惠券- 当前购物车是否满足优惠券使用条件
     * 3）验证价格
     *
     * @param orderItemList
     * @param confirmOrderRequest
     */
    private void checkPrice(List<OrderItemVO> orderItemList, ConfirmOrderRequest confirmOrderRequest) {
//    1）统计商品总价格
        BigDecimal realPayPrice = new BigDecimal(0);
        if (orderItemList != null) {
            for (OrderItemVO orderItemVO : orderItemList) {
                BigDecimal itemPrice = orderItemVO.getProductTotalPrice();
//                统计商品总价格
                realPayPrice = realPayPrice.add(itemPrice);
            }
        }
//        2）获取优惠券，判断是否可以使用(从时间角度上考虑可用性)(这里的优惠券，好像只能用一张)
        CouponRecordVO couponRecordVO = getCartCouponRecord(confirmOrderRequest.getCouponRecordId());
//        3） 验证价格
//        计算购物车价格， 是否满足优惠券满减条件
        if (couponRecordVO != null) {
//            计算是否使用该优惠券
            if (realPayPrice.compareTo(couponRecordVO.getConditionPrice()) < 0) {
                throw new BizException(BizCodeEnum.ORDER_CONFIRM_COUPON_FAIL);
            }
//            优惠券的优惠价格 大于 商品总价格， 就将下单价格设置为0
            if (couponRecordVO.getPrice().compareTo(realPayPrice) > 0) {
                realPayPrice = BigDecimal.ZERO;
            } else {
                realPayPrice = realPayPrice.subtract(couponRecordVO.getPrice());
            }

//            3-2) 判断优惠券使用后，与前端显示的价格（此时前端已经使用完了优惠券）是否一致
            if (realPayPrice.compareTo(confirmOrderRequest.getRealPayPrice()) != 0) {
                log.error("优惠券使用后，与前端显示的价格不一致");
                throw new BizException(BizCodeEnum.ORDER_CONFIRM_PRICE_FAIL);
            }
        }

    }

    /**
     * 获取用户优惠券
     *
     * @param couponRecordId
     * @return
     */
    private CouponRecordVO getCartCouponRecord(Long couponRecordId) {
        if (couponRecordId == null || couponRecordId < 0) {
            return null;
        }
//        根据RPC调用优惠券模块获取优惠券信息
        JsonData jsonData = couponFeignService.findUserCouponRecordById(couponRecordId);
        if (jsonData.getCode() != 0) {
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_COUPON_FAIL);
        }
        CouponRecordVO couponRecordVO = jsonData.getData(new TypeReference<>() {
        });
        if (!couponAvailable(couponRecordVO)) {
            throw new BizException(BizCodeEnum.COUPON_UNAVAILABLE);
        }
        return couponRecordVO;

    }

    /**
     * 判断优惠券是否可用
     *
     * @param couponRecordVO
     * @return
     */
    private boolean couponAvailable(CouponRecordVO couponRecordVO) {
//        时间--从时间上判断优惠券是否可用
        if (couponRecordVO.getUseState().equalsIgnoreCase(CouponStateEnum.NEW.name())) {
            long currentTimeStamp = CommonUtil.getCurrentTimeStamp();
            long end = couponRecordVO.getEndTime().getTime();
            long start = couponRecordVO.getStartTime().getTime();
            if (currentTimeStamp >= start && currentTimeStamp <= end) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取用户地址信息
     *
     * @param addressId
     * @return
     */
    private ProductOrderAddressVO getUserAddress(long addressId) {
        JsonData jsonData = userFeignService.detail(addressId);
        if (jsonData.getCode() != 0) {
            log.error("查询用户地址失败");
            throw new BizException(BizCodeEnum.ADDRESS_NO_EXITS);
        }
        ProductOrderAddressVO addressVO = jsonData.getData(new TypeReference<>() {
        });
        return addressVO;

    }

    /**
     * 查询订单状态
     *
     * @param outTradeNo
     * @return
     */
    @Override
    public String queryProductOrderState(String outTradeNo) {
        ProductOrderDO productOrderDO = productOrderMapper.selectOne(new QueryWrapper<ProductOrderDO>().eq("out_trade_no", outTradeNo));
        if (productOrderDO == null) {
            return "";
        } else {
            return productOrderDO.getState();
        }
    }

    /**
     * 关闭订单(利用MQ延迟消息实现)
     * @param orderMessage
     * @return
     */
    @Override
    public boolean closeProductOrder(OrderMessage orderMessage) {
        ProductOrderDO productOrderDO = productOrderMapper.selectOne(
                new QueryWrapper<ProductOrderDO>().eq("out_trade_no", orderMessage.getOutTradeNo())
        );
        if (productOrderDO == null) {
//            订单不存在
            log.warn("直接确认消息，订单不存在：{}",orderMessage);
            return true;
        }
//        订单支付成功
        if (productOrderDO.getState().equalsIgnoreCase(ProductOrderStateEnum.PAY.name())){
            log.info("直接确认消息，订单已支付：{}",orderMessage);
            return true;
        }

//        向第三方支付查询该订单是否真的未支付 TODO
        String payResult = "";
//        结果为空，表示支付失败，回滚，本地取消订单
        if (StringUtils.isBlank(payResult)) {
            productOrderMapper.updateOrderPayState(productOrderDO.getOutTradeNo(), ProductOrderStateEnum.CANCEL.name(),ProductOrderStateEnum.NEW.name());
            log.info("结果为空，未支付成功，本地取消订单");
            return true;
        }else {
//            支付成功， 主动把订单状态改成UI支付，造成该原因的情况可能时支付通道回调有问题
            log.warn("支付成功， 主动把订单状态改成UI支付，造成该原因的情况可能时支付通道回调有问题");
            productOrderMapper.updateOrderPayState(productOrderDO.getOutTradeNo(), ProductOrderStateEnum.PAY.name(),ProductOrderStateEnum.NEW.name());
            return true;
        }

    }
}
