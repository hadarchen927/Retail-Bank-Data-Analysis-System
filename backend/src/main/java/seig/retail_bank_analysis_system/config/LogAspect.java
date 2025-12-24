package seig.retail_bank_analysis_system.config;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import seig.retail_bank_analysis_system.util.JwtUtils;
import seig.retail_bank_analysis_system.util.Log;

import java.lang.reflect.Method;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Around("@annotation(seig.retail_bank_analysis_system.util.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = point.proceed();
        long time = System.currentTimeMillis() - beginTime;

        saveLog(point, time);
        return result;
    }

    private void saveLog(ProceedingJoinPoint joinPoint, long time) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Log logAnnotation = method.getAnnotation(Log.class);

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String username = "匿名用户"; // 给个默认值

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    try {
                        String token = authHeader.replace("Bearer ", "");
                        username = JwtUtils.getUsername(token); // 使用工具类解析
                    } catch (Exception e) {
                        username = "无效Token用户";
                    }
                }
            }

            String operation = logAnnotation != null ? logAnnotation.value() : "未知操作";
            String methodName = joinPoint.getTarget().getClass().getSimpleName() + "." + signature.getName();

            String sql = "INSERT INTO sys_log(username, operation, method, time, create_time) VALUES (?, ?, ?, ?, NOW())";
            jdbcTemplate.update(sql, username, operation, methodName, time);

        } catch (Exception e) {
            // 即使日志记录失败，也不要影响主业务运行
            System.err.println(">>> 日志记录异常: " + e.getMessage());
        }
    }
}