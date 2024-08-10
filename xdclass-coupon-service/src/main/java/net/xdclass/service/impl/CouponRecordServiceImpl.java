package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.RabbitMQConfig;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.CouponStateEnum;
import net.xdclass.enums.ProductOrderStateEnum;
import net.xdclass.enums.StockTaskStateEnum;
import net.xdclass.exception.BizException;
import net.xdclass.feign.ProductOrderFeignSerivce;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.CouponRecordMapper;
import net.xdclass.mapper.CouponTaskMapper;
import net.xdclass.model.CouponRecordDO;
import net.xdclass.model.CouponRecordMessage;
import net.xdclass.model.CouponTaskDO;
import net.xdclass.model.LoginUser;
import net.xdclass.request.LockCouponRecordRequest;
import net.xdclass.service.CouponRecordService;
import net.xdclass.util.JsonData;
import net.xdclass.vo.CouponRecordVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CouponRecordServiceImpl implements CouponRecordService {

    @Autowired
    private CouponRecordMapper couponRecordMapper;
    @Autowired
    private CouponTaskMapper couponTaskMapper;
    @Autowired
    private RabbitMQConfig rabbitMQConfig;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ProductOrderFeignSerivce productOrderFeignSerivce;

    /**
     * 分页查询个人的优惠券列表
     *
     * @param page
     * @param size
     * @return
     */
    @Override
    public Map<String, Object> page(int page, int size) {
//        通过拦截器 获取个人用户信息
        LoginUser loginUser = LoginInterceptor.threadLocal.get();

//        分页模板---直接套用即可
        Page<CouponRecordDO> pageInfo = new Page<>(page, size);
//        开始查询
        IPage<CouponRecordDO> recordDOPage = couponRecordMapper.selectPage(pageInfo, new QueryWrapper<CouponRecordDO>()
                .eq("user_id", loginUser.getId())
                .orderByDesc("create_time"));

        Map<String, Object> pageMap = new HashMap<>(3);

        pageMap.put("total_record", recordDOPage.getTotal());
        pageMap.put("total_page", recordDOPage.getPages());
        pageMap.put("current_data", recordDOPage.getRecords().stream().map(obj -> beanProcess(obj)).collect(Collectors.toList()));

        return pageMap;
    }

    /**
     * 用户根据优惠券id 查找优惠券信息
     *
     * @param recordId
     * @return
     */
    @Override
    public CouponRecordVO findeById(Long recordId) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
//     加入用户id ，防止水平权限攻击
        CouponRecordDO couponRecordDO = couponRecordMapper.selectOne(new QueryWrapper<CouponRecordDO>()
                .eq("id", recordId)
                .eq("user_id", loginUser.getId()));
        if (couponRecordDO == null) {
            return null;
        }
        return beanProcess(couponRecordDO);

    }

    /**
     * 锁定优惠券
     * 1）锁定优惠券记录
     * 2）将数据库中task表插入记录 订单号-与使用的优惠券进行绑定（之间进行锁定）
     * 3）发送延迟消息
     * @param recordRequest
     * @return
     */
    @Override
    public JsonData lockCouponRecords(LockCouponRecordRequest recordRequest) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        List<Long> lockCouponRecordIds = recordRequest.getLockCouponRecordIds();
        String orderOutTradeNo  = recordRequest.getOrderOutTradeNo();
//        1）锁定优惠券记录 -- 将用户所使用的优惠券状态修改为已使用
        int updateRows = couponRecordMapper.lockUseStateBatch(loginUser.getId(), CouponStateEnum.USED.name(),lockCouponRecordIds);
//        2） 将数据库中task--订单号-与使用的优惠券进行绑定（之间进行锁定）   表插入记录
        List<CouponTaskDO> couponTaskDOList =  lockCouponRecordIds.stream().map(obj->{
            CouponTaskDO couponTaskDO = new CouponTaskDO();
            couponTaskDO.setCreateTime(new Date());
            couponTaskDO.setOutTradeNo(orderOutTradeNo);
            couponTaskDO.setCouponRecordId(obj);
            couponTaskDO.setLockState(StockTaskStateEnum.LOCK.name());
            return couponTaskDO;
        }).collect(Collectors.toList());
//        2.2) 批量进行插入
        int insertRows = couponTaskMapper.insertBatch(couponTaskDOList);
        log.info("锁定优惠券记录：{},新增优惠券task表记录：{}",updateRows,insertRows);

//        3）发送延迟消息
        if (lockCouponRecordIds.size() == insertRows && insertRows==updateRows) {
            // 发送延迟消息
           for (CouponTaskDO couponTaskDO : couponTaskDOList){
               CouponRecordMessage couponRecordMessage = new CouponRecordMessage();
               couponRecordMessage.setOutTradeNo(couponTaskDO.getOutTradeNo());
               couponRecordMessage.setTaskId(couponTaskDO.getId());

//               rabbitMQ 发送延迟消息
               rabbitTemplate.convertAndSend(rabbitMQConfig.getEventExchange(),rabbitMQConfig.getCouponReleaseDelayRoutingKey(),couponRecordMessage);
               log.info("优惠券锁定消息发送成功:{}",couponRecordMessage.toString());
           }
            return JsonData.buildSuccess();
        }else {
            throw new BizException(BizCodeEnum.COUPON_RECORD_LOCK_FAIL);
        }
    }

    /**
     * 解锁优惠券记录 （整个逻辑 都是在一个事务里面，所以加个事务）
     * 1）查询task工作单是否存在
     * 2）如果存在，查询订单状态
     * @param couponRecordMessage
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public boolean releaseCouponRecord(CouponRecordMessage couponRecordMessage) {
        CouponTaskDO couponTaskDO = couponTaskMapper.selectOne(new QueryWrapper<CouponTaskDO>().eq("id",couponRecordMessage.getTaskId()));
        if (couponTaskDO == null) {
            log.warn("工作单不存在，消息为：{}",couponRecordMessage);
        }
//        只有lock状态才进行处理
        if (couponTaskDO.getLockState().equalsIgnoreCase(StockTaskStateEnum.LOCK.name())) {
//            查询该订单状态
            JsonData jsonData = productOrderFeignSerivce.queryProductOrderState(couponRecordMessage.getOutTradeNo());
            if (jsonData.getCode()==0) {
//                正常响应，判断订单的状态
                String state = jsonData.getData().toString();
                if (state.equalsIgnoreCase(ProductOrderStateEnum.NEW.name())) {
//                    状态为NEW新建状态， 则返回给消息队列， 继续重试
                    log.warn("订单状态为NEW, 返回给消息队列，重新投递：{}",couponRecordMessage);
                    return false;
                }
//                如果订单状态为已支付，将task工作单中的状态更改为finish
                if (state.equalsIgnoreCase(ProductOrderStateEnum.PAY.name())) {
                    couponTaskDO.setLockState(StockTaskStateEnum.FINISH.name());
//                    进行更新
                    couponTaskMapper.update(couponTaskDO,new QueryWrapper<CouponTaskDO>().eq("id",couponRecordMessage.getTaskId()));
                    log.info("订单已经完成支付，修改库存锁定工作单为FINISH状态");
                    return true;
                }
            }
//            订单不存在，或者订单被取消，确认消息，修改task的状态为CANCEL， 恢复优惠券使用记录为NEW，而不是USED
            log.warn("订单不存在，或者订单被取消，确认消息，修改task的状态为CANCEL， 恢复优惠券使用记录为NEW，而不是USED, message:{}",couponRecordMessage);
            couponTaskDO.setLockState(StockTaskStateEnum.CANCEL.name());
//            修改task的状态为CANCEL
            couponTaskMapper.update(couponTaskDO,new QueryWrapper<CouponTaskDO>().eq("id",couponRecordMessage.getTaskId()));
//            恢复优惠券使用记录为NEW，而不是USED
            couponRecordMapper.updateState(couponTaskDO.getCouponRecordId(),CouponStateEnum.NEW.name());
            return true;
        }else {
            log.warn("工作单不是LOCK，state={},消息体={}",couponTaskDO.getLockState(),couponRecordMessage);
            return true;
        }

    }

    private CouponRecordVO beanProcess(CouponRecordDO couponRecordDO) {
        CouponRecordVO couponRecordVO = new CouponRecordVO();
        BeanUtils.copyProperties(couponRecordDO, couponRecordVO);
        return couponRecordVO;
    }
}
