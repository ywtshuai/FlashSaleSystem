package com.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.dto.LoginRequest;
import com.flashsale.dto.RegisterRequest;
import com.flashsale.dto.UserInfoResponse;
import com.flashsale.dto.UserLoginResponse;
import com.flashsale.entity.User;
import com.flashsale.exception.BusinessException;
import com.flashsale.mapper.UserMapper;
import com.flashsale.service.UserService;
import com.flashsale.util.JwtUtil;
import com.flashsale.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * 注册时检查用户名唯一性，并使用 MD5 + Salt 加密密码。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        String salt = PasswordUtil.generateSalt();
        User user = new User();
        user.setUsername(request.getUsername());
        user.setSalt(salt);
        user.setPassword(PasswordUtil.encrypt(request.getPassword(), salt));
        userMapper.insert(user);
        return user.getId();
    }

    /**
     * 登录成功后返回标准 JWT。
     */
    @Override
    public UserLoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String encryptedPassword = PasswordUtil.encrypt(request.getPassword(), user.getSalt());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        UserLoginResponse response = new UserLoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setToken(jwtUtil.generateToken(user.getId(), user.getUsername()));
        response.setExpireAt(jwtUtil.getExpireAt());
        return response;
    }

    @Override
    public UserInfoResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setCreateTime(user.getCreateTime());
        return response;
    }
}
