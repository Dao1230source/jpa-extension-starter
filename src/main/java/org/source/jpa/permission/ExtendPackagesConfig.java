package org.source.jpa.permission;

import org.jetbrains.annotations.NotNull;
import org.source.jpa.HibernateConfig;
import org.source.spring.scan.ExtendPackagesProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;

import java.util.List;

@AutoConfigureBefore(HibernateConfig.class)
@AutoConfiguration
public class ExtendPackagesConfig {

    @Bean
    public ExtendPackagesProcessor permission() {
        return new ExtendPackagesProcessor() {
            @Override
            public @NotNull List<String> extendPackages() {
                return List.of(ClassUtils.getPackageName(ExtendPackagesConfig.class));
            }

            @Override
            public @NotNull Class<?> groupClass() {
                return HibernateConfig.class;
            }
        };
    }
}
