package net.xdclass.enums;

/**
 * 默认地址状态 的枚举类
 */
public enum AddressStatusEnum {

    /**
     * 是默认收货地址
     */
    DEFAULT_STATUS(1),

    /**
     * 不是默认收货地址
     */
    COMMON_STATUS(0);
    private int status;

    private AddressStatusEnum(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
