package seig.retail_bank_analysis_system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.SysLogMapper;
import seig.retail_bank_analysis_system.dao.UserMapper;
import seig.retail_bank_analysis_system.entity.SysLog;
import seig.retail_bank_analysis_system.entity.User;
import seig.retail_bank_analysis_system.util.Log;
import seig.retail_bank_analysis_system.util.Result;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private SysLogMapper sysLogMapper;
    @GetMapping("/logs")
    public Result<?> getLogs() {
        // 获取最近的 200 条操作日志，按时间倒序排列
        return Result.success(sysLogMapper.selectList(new LambdaQueryWrapper<SysLog>()
                .orderByDesc(SysLog::getCreateTime)
                .last("LIMIT 200")));
    }

    @GetMapping("/list")
    public Result<?> list() {
        return Result.success(userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getId)));
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody User user) {
        try {
            if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) > 0) {
                return Result.error("用户名重复");
            }
            // 关键：管理员新增用户也要加密密码
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setCreateTime(LocalDateTime.now());
            userMapper.insert(user);
            return Result.success("用户添加成功");
        } catch (Exception e) {
            e.printStackTrace(); // 这样你在IDEA控制台能看到报错详情
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    @PutMapping("/update-profile")
    public Result<?> updateProfile(@RequestBody Map<String, String> data) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, data.get("username")));
        if (user == null) return Result.error("用户不存在");

        user.setNickname(data.get("nickname"));
        if (data.get("password") != null && !data.get("password").isEmpty()) {
            user.setPassword(passwordEncoder.encode(data.get("password")));
        }
        userMapper.updateById(user);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @Log("清空所有系统日志")
    @DeleteMapping("/logs/clear")
    public Result<?> clearLogs() {
        try {
            // 使用 MyBatis-Plus 的 Wrapper 删除全表数据
            sysLogMapper.delete(null);
            return Result.success("日志已成功清空");
        } catch (Exception e) {
            return Result.error("清空失败：" + e.getMessage());
        }
    }
}