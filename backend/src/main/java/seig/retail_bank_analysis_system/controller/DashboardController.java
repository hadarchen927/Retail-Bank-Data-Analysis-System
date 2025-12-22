package seig.retail_bank_analysis_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.*;
import seig.retail_bank_analysis_system.util.Result;
import java.util.concurrent.TimeUnit;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private BankCustomerMapper customerMapper;
    @Autowired
    private BankTransactionMapper transactionMapper;

    // 【核心】注入 Redis 模板
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 用于对象序列化
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/all")
    public Result<?> getDashboardData() throws Exception {
        String cacheKey = "dashboard_data_cache";

        // 1. 尝试从 Redis 获取缓存
        String jsonCache = redisTemplate.opsForValue().get(cacheKey);
        if (jsonCache != null) {
            System.out.println(">>> 命中 Redis 缓存，正在返回缓存数据");
            // 将 JSON 转回 Map 返回
            Map<String, Object> cachedMap = objectMapper.readValue(jsonCache, Map.class);
            return Result.success(cachedMap);
        }

        // 2. 如果没有缓存，则查数据库
        System.out.println(">>> 未命中缓存，正在查询数据库...");
        Map<String, Object> result = new HashMap<>();
        result.put("stats", customerMapper.getGlobalStats());
        result.put("chartData", transactionMapper.getTransactionTypeStats());
        result.put("genderData", customerMapper.getGenderStats());
        result.put("trendData", transactionMapper.getMonthlyTrend());

        // 3. 将结果存入 Redis，并设置有效期为 10 分钟
        // 这样可以防止数据库被频繁访问
        String jsonResult = objectMapper.writeValueAsString(result);
        redisTemplate.opsForValue().set(cacheKey, jsonResult, 10, TimeUnit.MINUTES);

        return Result.success(result);
    }
}