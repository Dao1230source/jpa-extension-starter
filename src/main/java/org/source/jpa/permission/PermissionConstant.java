package org.source.jpa.permission;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PermissionConstant {
    public static final TransmittableThreadLocal<Boolean> PERMISSION_FLAG = TransmittableThreadLocal.withInitial(() -> Boolean.FALSE);
}
