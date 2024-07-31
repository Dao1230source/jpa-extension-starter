package org.source.jpa.repository;

import org.source.jpa.repository.extension.ExtensionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface UnifiedJpaRepository<T, ID> extends JpaRepository<T, ID>,
        JpaSpecificationExecutor<T>, ExtensionRepository<T> {
}
