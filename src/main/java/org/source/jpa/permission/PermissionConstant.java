package org.source.jpa.permission;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.experimental.UtilityClass;

/**
 * 权限控制常量类
 * <p>
 * 使用 TransmittableThreadLocal 存储权限过滤标志，支持多线程传递
 *
 * @author zengfugen
 */
@UtilityClass
public class PermissionConstant {

    /**
     * 权限过滤标志
     * <p>
     * true - 启用权限过滤
     * false - 禁用权限过滤
     */
    public static final TransmittableThreadLocal<Boolean> PERMISSION_FLAG = TransmittableThreadLocal.withInitial(() -> Boolean.FALSE);
}
