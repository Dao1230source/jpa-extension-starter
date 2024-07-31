package org.source.jpa.permission;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.source.jpa.enhance.FilterConstant;
import org.source.spring.trace.TraceContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.source.jpa.permission.PermissionConstant.PERMISSION_FLAG;

@Slf4j
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfiguration
public class FilterConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager(ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        JpaTransactionManager transactionManager = new JpaTransactionManager() {

            /**
             * <pre>
             * {@code @Filter} 必须在事物中{@code @Transactional(readOnly = true)}才会生效
             * eg: {@code @Transactional(readOnly = true)
             * public abstract class AbstractJpaHelper<T, ID> {}
             * }
             * </pre>
             * @return EntityManager
             */
            @Override
            protected @Nonnull EntityManager createEntityManagerForTransaction() {
                EntityManager entityManager = super.createEntityManagerForTransaction();
                if (Boolean.TRUE.equals(PERMISSION_FLAG.get())) {
                    Session session = entityManager.unwrap(Session.class);
                    String userId = TraceContext.getUserId();
                    if (log.isDebugEnabled()) {
                        log.debug("EntityManager set permission userId:{}", userId);
                    }
                    session.enableFilter(FilterConstant.PERMISSION).setParameter(FilterConstant.USER_ID, userId);
                }
                return entityManager;
            }
        };
        transactionManagerCustomizers.ifAvailable(customizers -> customizers.customize(transactionManager));
        return transactionManager;
    }

}
