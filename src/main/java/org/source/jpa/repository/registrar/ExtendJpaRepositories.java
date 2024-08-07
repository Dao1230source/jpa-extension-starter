package org.source.jpa.repository.registrar;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;

import java.lang.annotation.*;

/**
 * 完全和 {@literal @EnableJpaRepositories} 一样
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ExtendJpaRepositoriesRegistrar.class)
public @interface ExtendJpaRepositories {

    String[] value() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};


    ComponentScan.Filter[] includeFilters() default {};


    ComponentScan.Filter[] excludeFilters() default {};


    String repositoryImplementationPostfix() default "Impl";


    String namedQueriesLocation() default "";

    QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;


    Class<?> repositoryFactoryBeanClass() default JpaRepositoryFactoryBean.class;


    Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;


    String entityManagerFactoryRef() default "entityManagerFactory";

    String transactionManagerRef() default "transactionManager";

    boolean considerNestedRepositories() default false;


    boolean enableDefaultTransactions() default true;


    BootstrapMode bootstrapMode() default BootstrapMode.DEFAULT;


    char escapeCharacter() default '\\';
}
