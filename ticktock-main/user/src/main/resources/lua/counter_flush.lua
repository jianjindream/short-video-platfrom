local aggKey = KEYS[1]
local counterKey = KEYS[2]

local fields = redis.call('HGETALL', aggKey)
if #fields == 0 then
    return 0
end

local schemaLen = 4
local fieldSize = 4
local targetLen = schemaLen * fieldSize
local max = 4294967295

local function normalize(s)
    if not s then
        return string.rep(string.char(0), targetLen)
    end
    local len = string.len(s)
    if len < targetLen then
        return s .. string.rep(string.char(0), targetLen - len)
    end
    return s
end

local function read32be(s, off)
    local b = {string.byte(s, off + 1, off + 4)}
    local n = 0
    for i = 1, 4 do
        n = n * 256 + (b[i] or 0)
    end
    return n
end

local function write32be(n)
    local t = {}
    for i = 4, 1, -1 do
        t[i] = n % 256
        n = math.floor(n / 256)
    end
    return string.char(unpack(t))
end

local cnt = normalize(redis.call('GET', counterKey))

for i = 1, #fields, 2 do
    local field = fields[i]
    local delta = tonumber(fields[i + 1])
    local idx = -1

    if field == 'follow' then
        idx = 0
    elseif field == 'follower' then
        idx = 1
    elseif field == 'total_favorited' then
        idx = 2
    elseif field == 'favorite_count' or field == 'favorited_count' then
        idx = 3
    elseif field == 'collect_count' then
        redis.call('HDEL', aggKey, field)
    end

    if idx >= 0 and delta and delta ~= 0 then
        local off = idx * fieldSize
        local v = read32be(cnt, off) + delta
        if v < 0 then
            v = 0
        end
        if v > max then
            v = max
        end
        local seg = write32be(v)
        cnt = string.sub(cnt, 1, off) .. seg .. string.sub(cnt, off + fieldSize + 1)

        local left = redis.call('HINCRBY', aggKey, field, -delta)
        if left == 0 then
            redis.call('HDEL', aggKey, field)
        end
    end
end

redis.call('SET', counterKey, cnt)
return redis.call('HLEN', aggKey)
