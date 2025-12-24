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

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/all")
    public Result<?> getDashboardData() {
        String cacheKey = "dashboard_data_cache";

        try {
            // 1. 尝试从 Redis 获取缓存
            String jsonCache = redisTemplate.opsForValue().get(cacheKey);
            if (jsonCache != null) {
                // 成功命中的日志（演示时在控制台展示）
                System.out.println(">>> [Redis] 成功命中 Dashboard 缓存数据");
                Map<String, Object> cachedMap = objectMapper.readValue(jsonCache, Map.class);
                return Result.success(cachedMap);
            }

            // 2. 如果没有缓存，则查数据库
            System.out.println(">>> [MySQL] 未命中缓存，正在执行全行多维数据统计...");
            Map<String, Object> result = new HashMap<>();

            // 基础指标 (客户数、资产总额、贷款总额)
            result.put("stats", customerMapper.getGlobalStats());

            // 交易类型分布数据 (饼图)
            result.put("chartData", transactionMapper.getTransactionTypeStats());

            // 性别分布数据 (环形图)
            result.put("genderData", customerMapper.getGenderStats());

            // 交易趋势数据 (折线图)
            result.put("trendData", transactionMapper.getMonthlyTrend());

            // 业务板块排行数据 (柱状图 - 营销分析)
            result.put("productData", transactionMapper.getProductSalesStats());

            // 3. 将结果序列化并存入 Redis
            // 设置 10 分钟有效期，模拟真实银行的高性能处理逻辑
            String jsonResult = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, jsonResult, 10, TimeUnit.MINUTES);

            return Result.success(result);

        } catch (Exception e) {
            // 如果 Redis 挂了，降级处理直接查库（这是一个体现系统健壮性的亮点）
            System.err.println(">>> [Warning] Redis 访问异常，已降级为数据库直接访问: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("stats", customerMapper.getGlobalStats());
            result.put("chartData", transactionMapper.getTransactionTypeStats());
            result.put("genderData", customerMapper.getGenderStats());
            result.put("trendData", transactionMapper.getMonthlyTrend());
            result.put("productData", transactionMapper.getProductSalesStats());
            return Result.success(result);
        }
    }
}