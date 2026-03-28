-- Fixed Window Algorithm Lua Script
-- 实现固定窗口计数器算法的原子限流操作
-- KEYS[1]: 限流Key
-- ARGV[1]: 限流阈值
-- ARGV[2]: 窗口大小（毫秒）
-- ARGV[3]: 当前时间戳（毫秒）
-- ARGV[4]: 请求的令牌数

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_size = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- 获取当前窗口开始时间
local window_start = math.floor(now / window_size) * window_size

-- 获取当前计数
local current = tonumber(redis.call('GET', key))
if current == nil then
    current = 0
end

-- 判断是否超过阈值
if current + requested <= limit then
    -- 增加计数
    local new_count = redis.call('INCRBY', key, requested)
    -- 设置过期时间
    redis.call('PEXPIRE', key, window_size)
    return {1, limit - new_count}
else
    -- 计算窗口剩余时间
    local ttl = redis.call('PTTL', key)
    return {0, ttl > 0 and ttl or window_size}
end