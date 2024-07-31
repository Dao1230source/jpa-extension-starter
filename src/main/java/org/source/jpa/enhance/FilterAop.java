package org.source.jpa.enhance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.source.jpa.enhance.annotation.UseFilter;
import org.source.jpa.enhance.enums.FilterEnum;
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
    public Object prefer(ProceedingJoinPoint point, UseFilter filter) throws Throwable {
        FilterEnum[] filters = filter.filter();
        String[] filterNames = Streams.of(filters).map(FilterEnum::getName).toArray(String[]::new);
        Session session = null;
        try {
            if (filters.length > 0) {
                session = em.unwrap(Session.class);
                for (String filterName : filterNames) {
                    session.enableFilter(filterName);
                }
            }
            return point.proceed();
        } catch (Throwable e) {
            log.error("proceed() execute exception after add filters.", e);
            throw e;
        } finally {
            if (Objects.nonNull(session)) {
                for (String filterName : filterNames) {
                    session.disableFilter(filterName);
                }
            }
        }
    }
}
