package seig.retail_bank_analysis_system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import seig.retail_bank_analysis_system.dao.UserMapper;
import seig.retail_bank_analysis_system.entity.User;
import seig.retail_bank_analysis_system.util.JwtUtils;
import seig.retail_bank_analysis_system.util.Result;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.bouncycastle.asn1.cms.CMSObjectIdentifiers.data;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> loginData) {

        String username = loginData.get("username");
        String password = loginData.get("password");

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        Map<String, Object> data = new HashMap<>();
        // 【修改点】Token 格式改为：用户名_时间戳
        String token = JwtUtils.createToken(user.getUsername()); // 使用 JWT 工具
        data.put("token", token);

        data.put("role", user.getRole());
        data.put("nickname", user.getNickname());
        data.put("username", user.getUsername());
        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) > 0) {
            return Result.error("用户名已存在");
        }
        // 注册时必须加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success("注册成功");
    }
}