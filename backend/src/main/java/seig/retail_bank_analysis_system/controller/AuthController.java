package seig.retail_bank_analysis_system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seig.retail_bank_analysis_system.dao.UserMapper;
import seig.retail_bank_analysis_system.entity.User;
import seig.retail_bank_analysis_system.util.Result;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (user == null || !user.getPassword().equals(password)) {
            return Result.error("用户名或密码错误");
        }

        // 登录成功，返回 Token、角色和昵称
        Map<String, Object> data = new HashMap<>();
        data.put("token", "JWT_TOKEN_" + System.currentTimeMillis());
        data.put("role", user.getRole());       // 重要：返回角色
        data.put("nickname", user.getNickname()); // 重要：返回昵称

        return Result.success(data);
    }
}