package org.source.jpa.permission;

import com.alibaba.ttl.TransmittableThreadLocal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.source.utility.utils.Streams;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 权限控制 AOP 切面
 * <p>
 * 处理 @Permission 注解，管理权限过滤状态。
 * 使用 Deque 支持嵌套调用，通过 logicalAnd 计算最终的权限状态。
 *
 * <pre>
 * 工作流程：
 * 1. 方法标记 @Permission(verify = true)
 * 2. 切面设置权限标志到 ThreadLocal
 * 3. 事务管理器根据标志启用 PERMISSION Filter
 * 4. 方法执行完成后清除标志
 * </pre>
 *
 * @author zengfugen
 */
@RequiredArgsConstructor
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Aspect
@AutoConfiguration
public class PermissionAop {

    /**
     * 权限状态栈（支持嵌套调用）
     */
    private static final TransmittableThreadLocal<Deque<Boolean>> ENABLE_PERMISSION
            = TransmittableThreadLocal.withInitial(ArrayDeque::new);

    @PersistenceContext
    private EntityManager em;

    /**
     * 处理 @Permission 注解的方法
     * <p>
     * 设置权限过滤状态，支持嵌套调用（使用栈计算 logicalAnd）
     *
     * @param point      切点
     * @param permission @Permission 注解实例
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around(value = "@annotation(permission)")
    public Object prefer(ProceedingJoinPoint point, Permission permission) throws Throwable {
        Session session = em.unwrap(Session.class);
        Deque<Boolean> deque = ENABLE_PERMISSION.get();
        try {
            deque.addFirst(permission.verify());
            PermissionConstant.PERMISSION_FLAG.set(Streams.of(deque).reduce(Boolean::logicalAnd).orElse(Boolean.FALSE));
            return point.proceed();
        } finally {
            deque.pollFirst();
            session.disableFilter(FilterConstant.PERMISSION);
        }
    }
}
