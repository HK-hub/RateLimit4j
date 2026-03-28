package com.geek.ratelimit4j.core.algorithm;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 限流算法类型枚举
 * 定义所有支持的限流算法类型，用于配置和算法选择
 * 
 * <p>算法特点对比：</p>
 * <ul>
 *   <li>令牌桶：允许突发流量，适合间歇性流量波动</li>
 *   <li>漏桶：恒定流出速率，适合需要稳定流速的场景</li>
 *   <li>固定窗口：简单高效，存在临界时刻双倍流量问题</li>
 *   <li>滑动窗口日志：精确控制，内存开销较大</li>
 *   <li>滑动窗口计数器：平衡精度与性能</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public enum AlgorithmType {

    /**
     * 令牌桶算法 (Token Bucket)
     * 
     * <p>原理：以固定速率向桶中添加令牌，请求需要获取令牌才能通过。</p>
     * <p>特点：</p>
     * <ul>
     *   <li>允许突发流量处理</li>
     *   <li>桶满时丢弃多余令牌</li>
     *   <li>适合处理间歇性流量波动</li>
     * </ul>
     */
    TOKEN_BUCKET("token_bucket"),

    /**
     * 漏桶算法 (Leaky Bucket)
     * 
     * <p>原理：请求以恒定速率流出，桶满时拒绝新请求。</p>
     * <p>特点：</p>
     * <ul>
     *   <li>平滑输出请求流量</li>
     *   <li>恒定流出速率</li>
     *   <li>适合需要稳定流速的场景</li>
     * </ul>
     */
    LEAKY_BUCKET("leaky_bucket"),

    /**
     * 固定窗口计数器 (Fixed Window Counter)
     * 
     * <p>原理：将时间划分为固定大小窗口，在每个窗口内统计请求数量。</p>
     * <p>特点：</p>
     * <ul>
     *   <li>实现简单高效</li>
     *   <li>超过阈值则拒绝请求</li>
     *   <li>存在临界时刻双倍流量问题</li>
     * </ul>
     */
    FIXED_WINDOW("fixed_window"),

    /**
     * 滑动窗口日志 (Sliding Window Log)
     * 
     * <p>原理：记录每次请求的时间戳，统计最近时间窗口内的请求数。</p>
     * <p>特点：</p>
     * <ul>
     *   <li>精确控制请求速率</li>
     *   <li>不存在边界问题</li>
     *   <li>内存开销较大（需存储时间戳）</li>
     * </ul>
     */
    SLIDING_WINDOW_LOG("sliding_window_log"),

    /**
     * 滑动窗口计数器 (Sliding Window Counter)
     * 
     * <p>原理：结合多个固定窗口，使用加权平均计算当前窗口请求数。</p>
     * <p>特点：</p>
     * <ul>
     *   <li>平衡精度与性能</li>
     *   <li>临界问题比固定窗口小</li>
     *   <li>内存开销适中</li>
     * </ul>
     */
    SLIDING_WINDOW_COUNTER("sliding_window_counter");

    /**
     * 算法代码标识
     * 用于配置文件中的算法指定，与YAML/JSON配置兼容
     */
    private final String code;

    /**
     * 构造算法类型枚举
     * 
     * @param code 算法代码标识，用于配置文件解析
     */
    AlgorithmType(String code) {
        this.code = code;
    }

    /**
     * 获取算法代码标识
     * 
     * @return 算法代码字符串，如 "token_bucket"
     */
    public String getCode() {
        return this.code;
    }

    /**
     * 根据代码标识获取算法类型
     * 
     * <p>支持多种格式匹配：</p>
     * <ul>
     *   <li>代码标识：如 "token_bucket"</li>
     *   <li>枚举名称：如 "TOKEN_BUCKET"</li>
     *   <li>混合格式：自动转换为标准格式</li>
     * </ul>
     *
     * @param code 算法代码标识或枚举名称
     * @return 对应的算法类型，如果未找到则返回null
     */
    public static AlgorithmType fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }

        String normalizedCode = code.toLowerCase().trim();
        for (AlgorithmType type : values()) {
            if (Objects.equals(type.code, normalizedCode)) {
                return type;
            }
        }

        try {
            return valueOf(code.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 判断给定的代码标识是否为有效的算法类型
     * 
     * @param code 算法代码标识
     * @return true表示有效，false表示无效
     */
    public static boolean isValidCode(String code) {
        return Objects.nonNull(fromCode(code));
    }
}