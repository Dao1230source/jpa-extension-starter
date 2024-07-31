package org.source.jpa.repository.extension;

import java.util.Collection;

public interface ExtensionRepository<T> {

    /**
     * mysql onDuplicateUpdateBatch
     *
     * @param ts ts
     * @return ts
     */
    int onDuplicateUpdateBatch(Collection<T> ts);
}
