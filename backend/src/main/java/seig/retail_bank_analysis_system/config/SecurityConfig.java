package seig.retail_bank_analysis_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 必须禁用 CSRF，否则 POST 请求会 403
                .csrf(csrf -> csrf.disable())
                // 2. 启用跨域支持
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 3. 配置权限规则
                .authorizeHttpRequests(auth -> auth
                        // 允许所有人访问登录接口
                        .requestMatchers("/api/auth/login").permitAll()

                        // 允许访问仪表盘相关接口
                        .requestMatchers("/api/dashboard/**").permitAll()

                        // 允许访问数据管理相关接口 (导入/列表)
                        .requestMatchers("/api/data/**").permitAll()

                        // 允许访问用户管理相关接口
                        .requestMatchers("/api/user/**").permitAll()

                        // 【新增这一行】允许访问算法分析相关接口
                        .requestMatchers("/api/analysis/**").permitAll()

                        // 暂时允许其他所有请求，方便开发阶段测试
                        // 等系统全部完成后，这里通常会改为 .anyRequest().authenticated()
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    // 在 Security 层面配置跨域，这比 WebConfig 更稳
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