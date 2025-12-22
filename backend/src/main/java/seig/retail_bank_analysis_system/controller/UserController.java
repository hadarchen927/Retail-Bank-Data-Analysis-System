package seig.retail_bank_analysis_system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.UserMapper;
import seig.retail_bank_analysis_system.entity.User;
import seig.retail_bank_analysis_system.util.Result;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    // 1. 查询所有用户
    @GetMapping("/list")
    public Result<?> list() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByDesc(User::getId)); // 按ID倒序排列
        return Result.success(users);
    }

    // 2. 新增用户 (注册)
    @PostMapping("/add")
    public Result<?> add(@RequestBody User user) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            return Result.error("用户名已存在");
        }
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success("用户添加成功");
    }

    // 3. 修改用户信息
    @PutMapping("/update")
    public Result<?> update(@RequestBody User user) {
        if (user.getId() == null) return Result.error("ID不能为空");
        userMapper.updateById(user);
        return Result.success("更新成功");
    }

    // 4. 删除用户
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        // 禁止删除 admin 账号
        User user = userMapper.selectById(id);
        if ("admin".equals(user.getUsername())) {
            return Result.error("演示环境禁止删除管理员账号");
        }
        userMapper.deleteById(id);
        return Result.success("删除成功");
    }
}