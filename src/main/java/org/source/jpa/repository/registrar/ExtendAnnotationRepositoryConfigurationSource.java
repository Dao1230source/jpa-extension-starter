package org.source.jpa.repository.registrar;


import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.util.Streamable;

import java.lang.annotation.Annotation;

public class ExtendAnnotationRepositoryConfigurationSource extends AnnotationRepositoryConfigurationSource {
    private final BeanDefinitionRegistry registry;

    public ExtendAnnotationRepositoryConfigurationSource(AnnotationMetadata metadata,
                                                         Class<? extends Annotation> annotation,
                                                         ResourceLoader resourceLoader,
                                                         Environment environment,
                                                         BeanDefinitionRegistry registry,
                                                         BeanNameGenerator generator) {
        super(metadata, annotation, resourceLoader, environment, registry, generator);
        this.registry = registry;
    }

    @Override
    public Streamable<String> getBasePackages() {
        Streamable<String> basePackages = super.getBasePackages();
        // if (this.registry instanceof BeanFactory beanFactory) {
        //     List<String> packagesWithApp = ExtendPackagesProcessor.getPackagesWithApp(beanFactory, ExtendJpaRepositories.class);
        //     return basePackages.and(packagesWithApp);
        // }
        return basePackages;
    }
}
