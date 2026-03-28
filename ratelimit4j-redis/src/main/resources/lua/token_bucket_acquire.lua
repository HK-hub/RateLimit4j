-- Token Bucket Algorithm Lua Script
-- 实现令牌桶算法的原子限流操作
-- KEYS[1]: 限流Key
-- ARGV[1]: 最大令牌数
-- ARGV[2]: 每毫秒补充的令牌数
-- ARGV[3]: 当前时间戳（毫秒）
-- ARGV[4]: 请求的令牌数
-- ARGV[5]: 过期时间（秒）

local key = KEYS[1]
local max_tokens = tonumber(ARGV[1])
local tokens_per_ms = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])
local ttl = tonumber(ARGV[5])

-- 获取当前令牌数和最后更新时间
local bucket = redis.call('HMGET', key, 'tokens', 'last_update')
local current_tokens = tonumber(bucket[1])
local last_update = tonumber(bucket[2])

-- 如果桶不存在，初始化
if current_tokens == nil then
    current_tokens = max_tokens
    last_update = now
end

-- 计算应补充的令牌数
local elapsed = now - last_update
if elapsed > 0 then
    local new_tokens = math.min(current_tokens + elapsed * tokens_per_ms, max_tokens)
    current_tokens = new_tokens
end

-- 判断是否有足够的令牌
if current_tokens >= requested then
    -- 消耗令牌
    current_tokens = current_tokens - requested
    redis.call('HMSET', key, 'tokens', current_tokens, 'last_update', now)
    redis.call('EXPIRE', key, ttl)
    return {1, math.floor(current_tokens)}
else
    -- 计算等待时间
    local wait_time = math.ceil((requested - current_tokens) / tokens_per_ms)
    return {0, wait_time}
end