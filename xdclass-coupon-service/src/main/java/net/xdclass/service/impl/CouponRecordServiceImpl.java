package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.xdclass.controller.CouponRecordController;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.CouponRecordMapper;
import net.xdclass.model.CouponRecordDO;
import net.xdclass.model.LoginUser;
import net.xdclass.service.CouponRecordService;
import net.xdclass.vo.CouponRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CouponRecordServiceImpl implements CouponRecordService {

    @Autowired
    private CouponRecordMapper couponRecordMapper;

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

    private CouponRecordVO beanProcess(CouponRecordDO couponRecordDO) {
        CouponRecordVO couponRecordVO = new CouponRecordVO();
        BeanUtils.copyProperties(couponRecordDO, couponRecordVO);
        return couponRecordVO;
    }
}
