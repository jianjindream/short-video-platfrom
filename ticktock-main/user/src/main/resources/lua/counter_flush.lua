--[[
  计数系统 Lua 原子折叠脚本
  
  KEYS[1] = 聚合桶 Hash Key (e.g. agg:ucount:{userId}:{timeSlot})
  KEYS[2] = 用户计数器 SDS Key (e.g. ucounter:{userId})
  
  三步严格执行:
  1. HGETALL 读取聚合桶里的所有增量
  2. BITFIELD 命令将增量累加到 SDS 计数器对应 offset
  3. DEL 删除该聚合桶
  
  BITFIELD offset 定义:
    0  -> follow_count     (u32)
    32 -> follower_count   (u32)
    64 -> total_favorited  (u32)
]]

local aggKey = KEYS[1]
local counterKey = KEYS[2]

-- Step 1: HGETALL
local fields = redis.call('HGETALL', aggKey)
if #fields == 0 then
    return 0
end

-- Step 2: BITFIELD INCRBY
for i = 1, #fields, 2 do
    local field = fields[i]
    local delta = tonumber(fields[i + 1])
    
    if delta and delta ~= 0 then
        local offset = -1
        if field == 'follow' then
            offset = 0
        elseif field == 'follower' then
            offset = 32
        elseif field == 'total_favorited' or field == 'favorited_count' then
            offset = 64
        end
        
        if offset >= 0 then
            redis.call('BITFIELD', counterKey, 'INCRBY', 'u32', tostring(offset), tostring(delta))
        end
    end
end

-- Step 3: DEL
redis.call('DEL', aggKey)

return 1
