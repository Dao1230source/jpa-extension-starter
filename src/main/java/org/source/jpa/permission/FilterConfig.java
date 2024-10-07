package org.source.jpa.permission;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
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

import java.util.Collection;

import static org.source.jpa.permission.PermissionConstant.PERMISSION_FLAG;

@AllArgsConstructor
@Slf4j
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfiguration
public class FilterConfig {
    private final PermissionProcessor permissionProcessor;

    @ConditionalOnMissingBean
    @Bean
    public PermissionProcessor permissionProcessor() {
        return new DefaultPermissionProcessor();
    }

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
                    Collection<String> spaceIds = permissionProcessor.getSpaceIdsByUserId(userId);
                    if (log.isDebugEnabled()) {
                        log.debug("filter permission by userId:{}, spaceIds:{}", userId, spaceIds);
                    }
                    session.enableFilter(FilterConstant.PERMISSION).setParameter(FilterConstant.SPACE_IDS, spaceIds);
                }
                return entityManager;
            }
        };
        transactionManagerCustomizers.ifAvailable(customizers -> customizers.customize(transactionManager));
        return transactionManager;
    }

}
