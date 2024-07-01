package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.AddressStatusEnum;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.AddressMapper;
import net.xdclass.model.AddressDO;
import net.xdclass.model.LoginUser;
import net.xdclass.request.AddressAddRequest;
import net.xdclass.service.AddressService;
import net.xdclass.vo.AddressVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AddressServiceImpl implements AddressService {

    //    注入Mapper
    @Autowired
    private AddressMapper addressMapper;

    /**
     * 根据地址id 查询详细地址信息
     *
     * @param id
     * @return
     */
    @Override
    public AddressVO detail(Long id) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        AddressDO addressDO = addressMapper.selectOne(new QueryWrapper<AddressDO>().eq("id", id).eq("user_id", loginUser.getId()));

//        查询为空
        if (addressDO == null) {
            return null;
        }
//        否则
        AddressVO addressVO = new AddressVO();
        BeanUtils.copyProperties(addressDO, addressVO);

        return addressVO;
    }

    /**
     * 新增收货地址
     *
     * @param addressAddRequest
     */
    @Override
    public void add(AddressAddRequest addressAddRequest) {
//        先获取登录后Token所存的用户id，目的是将user 与对应的地址所绑定
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
//        声明AddressDao对象
        AddressDO addressDO = new AddressDO();
        addressDO.setCreateTime(new Date());
//        与User中用户进行绑定
        addressDO.setUserId(loginUser.getId());

        BeanUtils.copyProperties(addressAddRequest, addressDO);

//        是否有默认收货地址
        if (addressDO.getDefaultStatus() == AddressStatusEnum.DEFAULT_STATUS.getStatus()) {
//            1.先 查找数据库是否有默认地址
            AddressDO defaultAddressDo = addressMapper.selectOne(new QueryWrapper<AddressDO>().eq("user_id", addressDO.getUserId()).eq("default_status", AddressStatusEnum.DEFAULT_STATUS.getStatus()));
            if (defaultAddressDo != null) {
                //2. 将数据库中已存在的默认地址的数据 修改为非默认收货地址
                defaultAddressDo.setDefaultStatus(AddressStatusEnum.COMMON_STATUS.getStatus());
//                3. 更新数据库
                addressMapper.update(defaultAddressDo, new QueryWrapper<AddressDO>().eq("user_id", defaultAddressDo.getUserId()));
            }

        }
        int rows = addressMapper.insert(addressDO);

        log.info("新增收货地址： rows={}", rows);
    }

    /**
     * 删除指定收货地址
     *
     * @param id
     * @return
     */
    @Override
    public int del(Long id) {
//        在拦截器中获取 同一线程 中的用户对象 (根据token 生成)
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        int rows = addressMapper.delete(new QueryWrapper<AddressDO>().eq("id", id).eq("user_id", loginUser.getId()));
        return rows;
    }

    /**
     * 查询用户所有的收货地址
     *
     * @return
     */
    @Override
    public List<AddressVO> listUserAllAddress() {

        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        List<AddressDO> list = addressMapper.selectList(new QueryWrapper<AddressDO>().eq("user_id", loginUser.getId()));

//        *在List中迭代对象之间的转换*
        List<AddressVO> addressVOList = list.stream().map(obj -> {
            AddressVO addressVO = new AddressVO();
            BeanUtils.copyProperties(obj, addressVO);
            return addressVO;
        }).collect(Collectors.toList());

        return addressVOList;
    }
}
