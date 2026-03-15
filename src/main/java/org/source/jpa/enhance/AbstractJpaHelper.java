package org.source.jpa.enhance;

import lombok.Getter;
import org.source.jpa.repository.UnifiedJpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zengfugen
 */
@Getter
@Transactional(readOnly = true)
public abstract class AbstractJpaHelper<T, I> implements JpaHelper<T, I> {
    protected final UnifiedJpaRepository<T, I> repository;

    protected AbstractJpaHelper(UnifiedJpaRepository<T, I> repository) {
        this.repository = repository;
    }
}