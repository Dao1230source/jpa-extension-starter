package org.source.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA 基础配置
 * <p>
 * 配置 PersistenceManagedTypes，扫描实体类和 package-info.java 中的注解
 * 必须在 JpaBaseConfiguration 之前执行
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnClass(JpaRepository.class)
@AutoConfigureBefore(JpaBaseConfiguration.class)
@AutoConfiguration
public class JpaBaseConfig {

    @Primary
    @Bean
    public PersistenceManagedTypes persistenceManagedTypes(BeanFactory beanFactory, ResourceLoader resourceLoader) {
        List<String> packages = new ArrayList<>();

        // 1. starter 自己的包（扫描 package-info.java 中的 @FilterDef）
        packages.add(ClassUtils.getPackageName(JpaBaseConfig.class.getName()));

        // 2. 自动获取主项目的包（@SpringBootApplication 所在包）
        if (AutoConfigurationPackages.has(beanFactory)) {
            List<String> autoPackages = AutoConfigurationPackages.get(beanFactory);
            packages.addAll(autoPackages);
            log.debug("Auto-configuration packages: {}", autoPackages);
        }

        String[] packagesToScan = StringUtils.toStringArray(packages);
        return new PersistenceManagedTypesScanner(resourceLoader).scan(packagesToScan);
    }
}