package org.source.jpa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

/**
 * JPA Repository 配置
 * <p>
 * 使用 BeanPostProcessor 在 JpaRepositoryFactoryBean 初始化之前设置自定义 Repository 基类
 * 必须使用 postProcessBeforeInitialization，因为 afterPropertiesSet 在 postProcessAfterInitialization 之前执行
 */
@Slf4j
@ConditionalOnClass(JpaRepositoryFactoryBean.class)
@AutoConfigureBefore(JpaRepositoriesAutoConfiguration.class)
@AutoConfiguration
public class JpaRepositoriesConfig implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof JpaRepositoryFactoryBean<?, ?, ?> jpaRepositoryFactoryBean) {
            jpaRepositoryFactoryBean.setRepositoryBaseClass(ExtendedRepositoryImpl.class);
            log.info("RepositoryBaseClass set to: {} for bean: {}", ExtendedRepositoryImpl.class.getName(), beanName);
        }
        return bean;
    }
}