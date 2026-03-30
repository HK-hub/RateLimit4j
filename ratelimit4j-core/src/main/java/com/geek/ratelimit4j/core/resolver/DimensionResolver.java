package com.geek.ratelimit4j.core.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;

/**
 * 维度解析器接口
 * 每个维度对应一个实现类，遵循单一职责原则
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>一个解析器只负责一个维度的解析</li>
 *   <li>通过DimensionType关联具体实现类</li>
 *   <li>支持用户自定义替换任意维度实现</li>
 * </ul>
 *
 * <p>内置维度类型：</p>
 * <ul>
 *   <li>IP - IP地址解析</li>
 *   <li>USER - 用户ID解析</li>
 *   <li>TENANT - 租户ID解析</li>
 *   <li>DEVICE - 设备ID解析</li>
 *   <li>METHOD - 方法全限定名</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface DimensionResolver {

    /**
     * 获取支持的维度类型
     * 用于解析器注册和查找
     *
     * @return 维度类型
     */
    DimensionType getType();

    /**
     * 解析维度值
     *
     * @param context 解析上下文
     * @return 维度值，解析失败返回null
     */
    String resolve(DimensionResolveContext context);
}