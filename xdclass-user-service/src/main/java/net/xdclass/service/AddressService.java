package net.xdclass.service;

import net.xdclass.model.AddressDO;
import net.xdclass.request.AddressAddRequest;
import net.xdclass.vo.AddressVO;

import java.util.List;

public interface AddressService {

    AddressVO detail(Long id);

    /**
     * 新增收货地址
     *
     * @param addressAddRequest
     */
    void add(AddressAddRequest addressAddRequest);

    /**
     * 删除收货地址
     *
     * @param id
     * @return
     */
    int del(Long id);

    /**
     * 查询用户所有的收货地址
     *
     * @return
     */
    List<AddressVO> listUserAllAddress();
}
