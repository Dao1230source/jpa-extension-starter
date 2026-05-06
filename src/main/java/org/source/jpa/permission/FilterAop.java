package org.source.jpa.permission;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.source.jpa.annotation.Filters;
import org.source.utility.utils.Streams;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Objects;

/**
 * Hibernate Filter AOP 切面
 * <p>
 * 处理 @Filters 注解，在方法执行期间启用指定的 Hibernate Filter。
 * 方法执行完成后自动禁用 Filter。
 *
 * <pre>
 * 工作流程：
 * 1. 方法标记 @Filters(filter = {"NOT_DELETED"})
 * 2. 切面在方法执行前启用 Filter
 * 3. 执行方法，查询自动过滤数据
 * 4. 方法执行完成后禁用 Filter
 * </pre>
 *
 * @author zengfugen
 */
@Slf4j
@RequiredArgsConstructor
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Aspect
@AutoConfiguration
public class FilterAop {

    @PersistenceContext
    private EntityManager em;

    /**
     * 处理 @Filters 注解的方法
     * <p>
     * 在方法执行前启用指定的 Filter，执行后禁用
     *
     * @param point 切点
     * @param filter @Filters 注解实例
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around(value = "@annotation(filter)")
    public Object enableFilters(ProceedingJoinPoint point, Filters filter) throws Throwable {
        Session session = null;
        try {
            if (Objects.nonNull(session = em.unwrap(Session.class))) {
                Streams.of(filter.filter()).forEach(session::enableFilter);
            }
            return point.proceed();
        } catch (Throwable e) {
            log.error("enableFilters exception.", e);
            throw e;
        } finally {
            if (Objects.nonNull(session)) {
                Streams.of(filter.filter()).forEach(session::disableFilter);
            }
        }
    }
}
