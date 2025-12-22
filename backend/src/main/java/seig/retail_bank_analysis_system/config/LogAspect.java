package seig.retail_bank_analysis_system.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import seig.retail_bank_analysis_system.util.Log;
import java.lang.reflect.Method;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private JdbcTemplate jdbcTemplate; // 直接用简单方式入库

    @Around("@annotation(seig.retail_bank_analysis_system.util.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        // 执行原本的方法
        Object result = point.proceed();
        // 执行时间
        long time = System.currentTimeMillis() - beginTime;

        saveLog(point, time);
        return result;
    }

    private void saveLog(ProceedingJoinPoint joinPoint, long time) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);

        String operation = logAnnotation.value();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();

        // 实际开发这里会从 Security 获取当前登录用户名，演示直接写 admin
        String sql = "INSERT INTO sys_log(username, operation, method, time, create_time) VALUES (?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, "admin", operation, className + "." + methodName + "()", time);
    }
}