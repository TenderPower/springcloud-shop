package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.CouponCategoryEnum;
import net.xdclass.enums.CouponPublishEnum;
import net.xdclass.enums.CouponStateEnum;
import net.xdclass.exception.BizException;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.CouponMapper;
import net.xdclass.mapper.CouponRecordMapper;
import net.xdclass.model.CouponDO;
import net.xdclass.model.CouponRecordDO;
import net.xdclass.model.LoginUser;
import net.xdclass.request.NewUserRequest;
import net.xdclass.service.CouponService;
import net.xdclass.util.CommonUtil;
import net.xdclass.util.JsonData;
import net.xdclass.vo.CouponVO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private CouponRecordMapper couponRecordMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    //    注入Redission分布式锁
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 分页查询优惠券
     *
     * @param page
     * @param size
     * @return
     */
    @Override
    public Map<String, Object> pageCouponActivity(int page, int size) {
//        分页场景的使用：
//        1.new Page对象
        Page<CouponDO> pageInfo = new Page<>(page, size);
//        2.开始查询 并返回结果
        IPage<CouponDO> couponDOIPage = couponMapper.selectPage(pageInfo, new QueryWrapper<CouponDO>()
                .eq("publish", CouponPublishEnum.PUBLISH)
                .eq("category", CouponCategoryEnum.PROMOTION)
                .orderByDesc("create_time"));
//        3.映射到Map中
        Map<String, Object> pageMap = new HashMap<>(3);
//        总条数
        pageMap.put("total_record", couponDOIPage.getTotal());
//        总页数
        pageMap.put("total_page", couponDOIPage.getPages());
        pageMap.put("current_data", couponDOIPage.getRecords()
//                将Do 转为Vo 传给前端
                .stream()
                .map(obj -> beanProcess(obj)).collect(Collectors.toList()));

        return pageMap;
    }


    /**
     * 领取优惠券
     * 1. 获取这个优惠券是否存在
     * 2. 校验优惠券是否可以领取：时间、库存、超出限制
     * 3. 扣减库存
     * 4. 保存记录
     *
     * @param couponId
     * @param couponCategoryEnum
     * @return
     */

//    本地事务配置步骤2--**方法增加注解 @Transactional(rollbackFor=Exception.class,propagation=Propagation.REQUIRED)**
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public JsonData getCoupon(long couponId, CouponCategoryEnum couponCategoryEnum) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
//        设置分布式锁的 key
        String lockKey = "lock:coupon:" + couponId;
//        使用Redission进行分布式加锁
        RLock lock = redissonClient.getLock(lockKey);
        //阻塞式等待，一个线程获取锁后，其他线程只能等待，和原生的方式循环调用不一样
        lock.lock();
        log.info("领券接口加锁成功：{}", Thread.currentThread().getId());
//          加锁成功 执行业务
        try {
//        执行业务
//        1.获取优惠券是否存在
            CouponDO couponDO = couponMapper.selectOne(new QueryWrapper<CouponDO>()
                    .eq("id", couponId)
                    .eq("category", couponCategoryEnum.name())
                    .eq("publish", CouponPublishEnum.PUBLISH));

//        2.判断优惠券是否可以领取
            this.checkCoupon(couponDO, loginUser.getId());

//        3. 构建用户领券记录
            CouponRecordDO couponRecordDO = new CouponRecordDO();
            BeanUtils.copyProperties(couponDO, couponRecordDO);
            couponRecordDO.setCreateTime(new Date());
            couponRecordDO.setUseState(CouponStateEnum.NEW.name());
            couponRecordDO.setUserId(loginUser.getId());
            couponRecordDO.setUserName(loginUser.getName());
            couponRecordDO.setCouponId(couponId);
//        因为上面copy 导致couponDo的id 与 couponRecordDo的id 是一样的
//        避免一样，要将couponRecordDo的id设置为null
            couponRecordDO.setId(null);

//        4.扣减库存
            int rows = couponMapper.reduceStock(couponId);
            if (rows == 1) {
                //库存扣减成功才保存
                couponRecordMapper.insert(couponRecordDO);
                log.warn("库存扣减成功才保存");
            } else {
                log.warn("发放优惠券失败:{},用户:{}", couponDO, loginUser);
                throw new BizException(BizCodeEnum.COUPON_NO_STOCK);
            }

        } finally {
//               解锁
            lock.unlock();
            log.info("解锁成功");
        }
        return JsonData.buildSuccess();
    }

    /**
     * 给新人领取 新人优惠券
     * 注意点：
     * 1. 用户微服务调用优惠券微服务时，并没有token，也就是说没有进行登录
     * 2. 本地直接调用领取优惠券接口，需要自己构建一个UserLogin对象，并存放在threadlocal
     * @param newUserRequest
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public JsonData initNewUserCoupon(NewUserRequest newUserRequest) {
//       本地直接调用领取优惠券接口，需要自己构建一个UserLogin对象，并存放在threadlocal
        LoginUser loginUser = LoginUser.builder().build();
        loginUser.setId(newUserRequest.getUserId());
        loginUser.setName(newUserRequest.getName());
        LoginInterceptor.threadLocal.set(loginUser);
//        查看数据库是否有新人的优惠券
        List<CouponDO> couponDOList = couponMapper.selectList(new QueryWrapper<CouponDO>()
                .eq("category", CouponCategoryEnum.NEW_USER.name()));
        for (CouponDO couponDO : couponDOList){
//            调用添加优惠券接口
            this.getCoupon(couponDO.getId(), CouponCategoryEnum.NEW_USER);

        }
        return JsonData.buildSuccess();
    }

    /**
     * 校验优惠券是否可以领取
     * @param couponDO
     * @param userId
     */
    private void checkCoupon(CouponDO couponDO, Long userId) {
        //优惠券不存在
        if (couponDO == null) {
            throw new BizException(BizCodeEnum.COUPON_NO_EXITS);
        }
        //库存不足
        if (couponDO.getStock() <= 0) {
            throw new BizException(BizCodeEnum.COUPON_NO_STOCK);
        }
//        用户是否超过了领取时间
        long time = CommonUtil.getCurrentTimeStamp();
        long start = couponDO.getStartTime().getTime();
        long end = couponDO.getEndTime().getTime();
        if (time < start || time > end) {
            throw new BizException(BizCodeEnum.COUPON_OUT_OF_TIME);
        }

//        用户是否超出了限制
        Long recordCount = couponRecordMapper.selectCount(
                new QueryWrapper<CouponRecordDO>()
                        .eq("coupon_id", couponDO.getId())
                        .eq("user_id", userId));

        if (recordCount >= couponDO.getUserLimit()) {
            throw new BizException(BizCodeEnum.COUPON_OUT_OF_LIMIT);
        }

    }

    /**
     * 将DO对象 转为 VO对象
     *
     * @param couponDO
     * @return
     */
    private Object beanProcess(CouponDO couponDO) {
        CouponVO couponVO = new CouponVO();
        BeanUtils.copyProperties(couponDO, couponVO);
        return couponVO;
    }
}
