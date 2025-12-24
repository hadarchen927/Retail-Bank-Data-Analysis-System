package seig.retail_bank_analysis_system.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import seig.retail_bank_analysis_system.util.Result;

/**
 * 全局异常处理器
 * 任务书研究内容 1.2 系统稳定性优化
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统运行出错: ", e);
        // 返回友好的错误提示，而不是原始的堆栈信息
        return Result.error("服务器繁忙或逻辑异常：" + e.getMessage());
    }

    /**
     * 特定处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNpe(NullPointerException e) {
        log.error("空指针异常: ", e);
        return Result.error("操作失败：系统检测到关键数据缺失。");
    }
}