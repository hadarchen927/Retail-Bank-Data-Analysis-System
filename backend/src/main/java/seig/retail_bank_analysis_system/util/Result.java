package seig.retail_bank_analysis_system.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一后端返回结果对象
 * 所有的后端接口都应该返回这个 Result 对象，以便前端统一处理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    // 状态码：200 成功，500 失败（可以根据需要扩展 401, 403 等）
    private Integer code;

    // 提示消息
    private String msg;

    // 实际承载的数据（可以是对象、列表、Map等）
    private T data;

    /**
     * 成功时的快捷返回方法（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功时的快捷返回方法（不带数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 失败时的快捷返回方法
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    /**
     * 自定义状态码的失败返回
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}