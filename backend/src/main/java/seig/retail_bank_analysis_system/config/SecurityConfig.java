package seig.retail_bank_analysis_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import java.util.Arrays;

@Configuration
public class SecurityConfig {

    // 【必须有这个】定义加密器供全系统使用
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF
                .csrf(csrf -> csrf.disable())
                // 2. 开启跨域
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 3. 配置权限规则 (注意：顺序由上到下，越具体的越靠前)
                .authorizeHttpRequests(auth -> auth
                        // 放行登录和注册
                        .requestMatchers("/api/auth/**").permitAll()

                        // 放行仪表盘统计接口
                        .requestMatchers("/api/dashboard/**").permitAll()

                        // 放行数据中心接口
                        .requestMatchers("/api/data/**").permitAll()

                        // 放行 AI 算法接口
                        .requestMatchers("/api/analysis/**").permitAll()

                        // 放行风险预警接口
                        .requestMatchers("/api/risk/**").permitAll()

                        // 放行 PDF 报告导出接口
                        .requestMatchers("/api/report/**").permitAll()

                        // 【关键】放行刚才新增的日志查询接口 (必须放在 anyRequest 之前)
                        .requestMatchers("/api/user/logs").permitAll()

                        // 放行用户管理相关接口
                        .requestMatchers("/api/user/**").permitAll()

                        // 放行清除日志缓存相关接口
                        .requestMatchers("/api/user/logs/clear").permitAll()

                        // 所有的规则都要在 anyRequest 之前定义
                        .anyRequest().permitAll()


                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}