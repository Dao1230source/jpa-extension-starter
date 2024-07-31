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
import org.source.jpa.enhance.FilterConstant;
import org.source.utility.utils.Streams;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayDeque;
import java.util.Deque;

@RequiredArgsConstructor
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Aspect
@AutoConfiguration
public class PermissionAop {
    private static final TransmittableThreadLocal<Deque<Boolean>> ENABLE_PERMISSION
            = TransmittableThreadLocal.withInitial(ArrayDeque::new);
    @PersistenceContext
    private EntityManager em;

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
