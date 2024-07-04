package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.mapper.BannerMapper;
import net.xdclass.model.BannerDO;
import net.xdclass.service.BannerService;
import net.xdclass.vo.BannerVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BannerServiceImpl implements BannerService {
    @Autowired
    private BannerMapper bannerMapper;

    /**
     * 轮播接口开发
     * @return
     */
    @Override
    public List<BannerVO> list() {
        List<BannerDO> list = bannerMapper.selectList(new QueryWrapper<BannerDO>()
                .orderByDesc("weight"));
        return list.stream().map(obj -> {
            BannerVO vo = new BannerVO();
            BeanUtils.copyProperties(obj, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
