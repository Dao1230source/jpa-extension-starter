package org.source.jpa.repository.registrar;

import org.jetbrains.annotations.NotNull;
import org.source.spring.scan.ExtendPackagesProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.*;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;

public class ExtendJpaRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {
    @Override
    protected @NotNull Class<? extends Annotation> getAnnotation() {
        return ExtendJpaRepositories.class;
    }

    @Override
    protected @NotNull RepositoryConfigurationExtension getExtension() {
        return new JpaRepositoryConfigExtension();
    }

    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void setResourceLoader(@NotNull ResourceLoader resourceLoader) {
        super.setResourceLoader(resourceLoader);
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(@NotNull Environment environment) {
        super.setEnvironment(environment);
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(@NotNull AnnotationMetadata metadata,
                                        @NotNull BeanDefinitionRegistry registry,
                                        @NotNull BeanNameGenerator generator) {
        Assert.notNull(metadata, "AnnotationMetadata must not be null");
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        Assert.notNull(resourceLoader, "ResourceLoader must not be null");
        // Guard against calls for sub-classes
        if (metadata.getAnnotationAttributes(getAnnotation().getName()) == null) {
            return;
        }
        // 扩展 AnnotationRepositoryConfigurationSource
        AnnotationRepositoryConfigurationSource configurationSource = new ExtendAnnotationRepositoryConfigurationSource(
                metadata, getAnnotation(), resourceLoader, environment, registry, generator);
        RepositoryConfigurationExtension extension = getExtension();
        RepositoryConfigurationUtils.exposeRegistration(extension, registry, configurationSource);
        RepositoryConfigurationDelegate delegate = new RepositoryConfigurationDelegate(
                configurationSource, resourceLoader, environment);
        delegate.registerRepositoriesIn(registry, extension);
        // 注册 bean
        if (registry instanceof BeanFactory beanFactory) {
            ExtendPackagesProcessor.extendPackagesProcessorList(beanFactory, ExtendJpaRepositories.class)
                    .forEach(k -> k.after(beanFactory));
        }
    }
}
