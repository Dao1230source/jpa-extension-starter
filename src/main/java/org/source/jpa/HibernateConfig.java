package org.source.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.source.jpa.enhance.AbstractJpaHelper;
import org.source.jpa.repository.registrar.ExtendJpaRepositories;
import org.source.spring.scan.ExtendPackagesProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 该配置主要是为了是jpa可以扫描到 "org.source.jpa"下”package-info.java“定义的 @FilterDef 以便在其被依赖的地方使用 hibernate Filter
 */
@Slf4j
@RequiredArgsConstructor
@ExtendJpaRepositories
@AutoConfigureBefore(value = {JpaBaseConfiguration.class})
@AutoConfiguration
public class HibernateConfig {

    @Primary
    @Bean
    public PersistenceManagedTypes persistenceManagedTypes(BeanFactory beanFactory, ResourceLoader resourceLoader) {
        List<String> packages = new ArrayList<>();
        // jpa-help-starter 的 @FilterDef定义在该包下
        packages.add(ClassUtils.getPackageName(AbstractJpaHelper.class.getName()));
        packages.addAll(ExtendPackagesProcessor.getPackagesWithApp(beanFactory, HibernateConfig.class));
        String[] packagesToScan = StringUtils.toStringArray(packages);
        return new PersistenceManagedTypesScanner(resourceLoader).scan(packagesToScan);
    }
}
