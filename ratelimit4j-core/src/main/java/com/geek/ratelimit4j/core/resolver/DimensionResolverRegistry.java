package com.geek.ratelimit4j.core.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维度解析器注册中心
 * 管理各维度解析器，根据维度类型获取对应解析器
 *
 * <p>设计特点：</p>
 * <ul>
 *   <li>每个维度一个解析器，单一职责</li>
 *   <li>通过DimensionType关联具体实现</li>
 *   <li>支持运行时动态注册</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class DimensionResolverRegistry {

    private final Map<DimensionType, DimensionResolver> resolvers = new ConcurrentHashMap<>();

    /**
     * 注册解析器
     *
     * @param resolver 维度解析器
     */
    public void register(DimensionResolver resolver) {
        if (Objects.isNull(resolver)) {
            return;
        }
        DimensionType type = resolver.getType();
        if (Objects.isNull(type)) {
            return;
        }
        resolvers.put(type, resolver);
    }

    /**
     * 获取指定维度的解析器
     *
     * @param type 维度类型
     * @return 解析器，不存在返回null
     */
    public DimensionResolver getResolver(DimensionType type) {
        return resolvers.get(type);
    }

    /**
     * 解析指定维度的值
     *
     * @param type    维度类型
     * @param context 解析上下文
     * @return 维度值
     */
    public String resolve(DimensionType type, DimensionResolveContext context) {
        DimensionResolver resolver = getResolver(type);
        if (Objects.isNull(resolver)) {
            return null;
        }
        return resolver.resolve(context);
    }

    /**
     * 判断指定维度是否有解析器
     *
     * @param type 维度类型
     * @return true表示存在
     */
    public boolean hasResolver(DimensionType type) {
        return resolvers.containsKey(type);
    }
}