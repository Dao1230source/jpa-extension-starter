package org.source.jpa;

import org.source.utility.exception.BaseException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

/**
 * 扩展的 JPA Repository 接口
 * <p>
 * 提供以下增强功能：
 * <ul>
 *     <li>批量插入/更新（ON DUPLICATE KEY UPDATE）</li>
 *     <li>逻辑删除和物理删除</li>
 *     <li>按条件删除和查询</li>
 *     <li>更新操作（自动合并非空字段）</li>
 * </ul>
 *
 * @param <T> 实体类型
 * @param <I> 主键类型
 * @author zengfugen
 */
@NoRepositoryBean
public interface ExtendedRepository<T, I> extends JpaRepository<T, I> {

    /**
     * 批量插入/更新
     * <p>
     * 使用 MySQL ON DUPLICATE KEY UPDATE 语法实现高性能批量操作：
     * <ul>
     *     <li>如果唯一键不匹配（记录不存在），则插入新记录</li>
     *     <li>如果唯一键匹配（记录已存在），则更新现有记录</li>
     * </ul>
     *
     * @param ts 实体集合
     * @return 影响的行数
     */
    int onDuplicateUpdateBatch(Collection<T> ts);

    /**
     * 按条件逻辑删除
     * <p>
     * 根据条件查找所有匹配记录，将标记为 @LogicDelete 的字段设置为删除状态
     *
     * @param condition 查询条件
     */
    void delete(Condition<T> condition);

    /**
     * 按主键物理删除
     * <p>
     * 直接从数据库删除记录，不经过逻辑删除
     *
     * @param id 主键
     */
    void removeById(I id);

    /**
     * 按实体物理删除
     *
     * @param t 实体对象
     */
    void remove(T t);

    /**
     * 按条件物理删除
     *
     * @param condition 查询条件
     */
    void remove(Condition<T> condition);

    /**
     * 更新实体（自定义合并逻辑）
     * <p>
     * 先从数据库查询记录，然后使用自定义合并函数合并字段
     *
     * @param t             输入的实体
     * @param dbEntityUpdate 合并函数 (数据库记录, 输入记录) -> 合并后的记录
     * @return 更新后的实体
     */
    T update(T t, BinaryOperator<T> dbEntityUpdate);

    /**
     * 更新实体（自动合并非空字段）
     * <p>
     * 从数据库查询记录，将输入实体中非空的字段合并到数据库记录
     *
     * @param t 输入的实体
     * @return 更新后的实体
     */
    T update(T t);

    /**
     * 按主键查询，不存在则抛出异常
     *
     * @param id               主键
     * @param exceptionSupplier 异常提供者
     * @return 实体对象
     */
    T get(I id, Supplier<BaseException> exceptionSupplier);

    /**
     * 按条件查询单条记录，不存在则抛出异常
     *
     * @param condition 查询条件
     * @return 实体对象
     */
    T get(Condition<T> condition);

    /**
     * 按条件查询全部记录
     *
     * @param condition 查询条件
     * @return 实体列表
     */
    List<T> findAll(Condition<T> condition);
}
