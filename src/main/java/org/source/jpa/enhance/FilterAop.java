package org.source.jpa.enhance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.source.jpa.enhance.annotation.Filters;
import org.source.utility.utils.Streams;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Aspect
@AutoConfiguration
public class FilterAop {
    @PersistenceContext
    private EntityManager em;

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
