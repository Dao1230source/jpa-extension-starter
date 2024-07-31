package org.source.jpa.repository.extension;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

/**
 * <pre>
 * 错误方式：
 * 如果在该配置类上添加 {@literal  @EnableJpaRepositories(repositoryBaseClass = ExtensionRepositoryImpl.class)}
 * 会因为运行时扫描的是{@literal org.source.jpa} 包，导致主程序下的 Repository 不能生成 bean
 * 正确方式：
 * 1、在使用该组件的系统的启动类上添加 {@literal @EnableJpaRepositories(repositoryBaseClass = ExtensionRepositoryImpl.class)}
 * 2、在 {@literal JpaRepositoryFactoryBean}也即是 {@literal Repository} 生成bean之前设置 repositoryBaseClass
 * </pre>
 */
@AutoConfigureBefore(value = {JpaRepositoriesAutoConfiguration.class})
@AutoConfiguration
public class RepositoryConfig implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        if (bean instanceof JpaRepositoryFactoryBean<?, ?, ?> jpaRepositoryFactoryBean) {
            jpaRepositoryFactoryBean.setRepositoryBaseClass(ExtensionRepositoryImpl.class);
        }
        return bean;
    }
}
