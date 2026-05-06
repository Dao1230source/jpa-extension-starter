package org.source.jpa.permission;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
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

/**
 * 权限过滤配置类
 * <p>
 * 配置事务管理器，在事务开始时根据权限标志启用 PERMISSION Filter。
 * 同时注册默认的 PermissionProcessor 实现。
 *
 * <pre>
 * 工作流程：
 * 1. PermissionAop 设置权限标志到 ThreadLocal
 * 2. 事务开始时，FilterConfig 检查权限标志
 * 3. 如果标志为 true，启用 PERMISSION Filter 并设置参数
 * 4. Filter 参数由 PermissionProcessor 提供
 * </pre>
 *
 * <p>注意：@Filter 必须在事务中才会生效，如 @Transactional(readOnly = true)</p>
 *
 * @author zengfugen
 */
@AllArgsConstructor
@Slf4j
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfiguration
public class FilterConfig {

    private final PermissionProcessor permissionProcessor;

    /**
     * 注册默认的权限处理器
     * <p>
     * 如果用户没有提供自定义实现，使用 DefaultPermissionProcessor
     *
     * @return PermissionProcessor 实例
     */
    @ConditionalOnMissingBean
    @Bean
    public PermissionProcessor permissionProcessor() {
        return new DefaultPermissionProcessor();
    }

    /**
     * 配置事务管理器
     * <p>
     * 自定义 JpaTransactionManager，在创建 EntityManager 时检查权限标志并启用 Filter
     *
     * @param transactionManagerCustomizers 事务管理器定制器
     * @return PlatformTransactionManager 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager(ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        JpaTransactionManager transactionManager = new JpaTransactionManager() {

            /**
             * 创建事务使用的 EntityManager
             * <p>
             * 如果权限标志为 true，启用 PERMISSION Filter 并设置 spaceIds 参数
             * <pre>
             * &#64;Filter 必须在事务中 &#64;Transactional(readOnly = true) 才会生效
             * </pre>
             *
             * @return EntityManager 实例
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
