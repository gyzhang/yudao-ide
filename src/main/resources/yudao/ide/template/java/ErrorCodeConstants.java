package cn.iocoder.yudao.module.$moduleNameShort.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.enums.ServiceErrorCodeRange;

/**
 * $moduleNameShort 模块错误码区间 [1-606-000-000 ~ 1-607-000-000)
 * @see ServiceErrorCodeRange
 * 请在项目全局视角下规划确认 $moduleNameShort 模块的错误码范围，请勿随意设置
 */
public interface ErrorCodeConstants {
    ErrorCode HELLO_WORLD_NOT_EXISTS = new ErrorCode(1_606_001_000, "代码生成验证不存在");
}
