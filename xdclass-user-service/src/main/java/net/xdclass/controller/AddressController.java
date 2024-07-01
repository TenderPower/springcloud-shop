package net.xdclass.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.exception.BizException;
import net.xdclass.model.AddressDO;
import net.xdclass.request.AddressAddRequest;
import net.xdclass.service.AddressService;
import net.xdclass.util.JsonData;
import net.xdclass.vo.AddressVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 电商-公司收发货地址表 前端控制器
 * </p>
 *
 * @author ygk
 * @since 2024-04-11
 */
@Api(tags = "收货地址模块")
@RestController
@RequestMapping("/api/address/v1/") //（api 是针对C端的）
public class AddressController {

    @Autowired
    AddressService addressService;

    /**
     * 根据id查找地址详情
     *
     * @param id
     * @return
     */
    @ApiOperation("根据id查找地址详情")
    @GetMapping("find/{address_id}")
    public JsonData detail(@ApiParam(value = "地址id", required = true)
                           @PathVariable("address_id") Long id) {//接受从前端传来的参数值
        AddressVO addressVO = addressService.detail(id);

//        测试自定义异常
//        if (id == 1) {
//            throw new BizException(-1,"测试自定义异常");
//        }

        return addressVO == null ?
                JsonData.buildResult(BizCodeEnum.ADDRESS_NO_EXITS)
                : JsonData.buildSuccess(addressVO); //这部目的是test 统一接口的响应（这是规范）
    }

    /**
     * 向用户USer 添加收货地址
     *
     * @param addressAddRequest
     * @return
     */
    @ApiOperation("新增收货地址")
    @PostMapping("add")
    public JsonData add(@ApiParam(value = "地址对象") AddressAddRequest addressAddRequest) {
//        地址添加
        addressService.add(addressAddRequest);

        return JsonData.buildSuccess();
    }

    @ApiOperation("删除收货地址")
    @DeleteMapping("del/{address_id}")
    public JsonData del(
            @ApiParam(value = "地址id", required = true)
            @PathVariable("address_id") Long id) {
        int row = addressService.del(id);
        return row == 1
                ? JsonData.buildSuccess()
                : JsonData.buildResult(BizCodeEnum.ADDRESS_DEL_FAIL);
    }

    /**
     * 查询用户所有的收货地址
     *
     * @return
     */
    @ApiOperation("查询用户所有的收货地址")
    @GetMapping("list")
    public JsonData findUserAllAddress() {
        List<AddressVO> list = addressService.listUserAllAddress();
        return JsonData.buildSuccess(list);
    }
}

