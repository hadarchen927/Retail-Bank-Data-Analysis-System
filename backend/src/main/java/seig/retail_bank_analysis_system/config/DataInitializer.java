package seig.retail_bank_analysis_system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import seig.retail_bank_analysis_system.dao.UserMapper;
import seig.retail_bank_analysis_system.entity.User;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. 检查是否存在 admin 账号
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));

        if (admin != null) {
            // 2. 如果存在，强制重置密码为 123456 的正确加密串
            admin.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(admin);
            System.out.println(">>> [系统初始化] 已自动重置 admin 密码为加密后的 123456");
        } else {
            // 3. 如果不存在，直接创建一个
            User newAdmin = new User();
            newAdmin.setUsername("admin");
            newAdmin.setPassword(passwordEncoder.encode("123456"));
            newAdmin.setRole("ADMIN");
            newAdmin.setNickname("超级管理员");
            newAdmin.setCreateTime(LocalDateTime.now());
            userMapper.insert(newAdmin);
            System.out.println(">>> [系统初始化] 检测到 admin 账号缺失，已自动创建");
        }
    }
}