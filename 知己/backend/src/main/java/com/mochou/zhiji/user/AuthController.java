package com.mochou.zhiji.user;

import com.mochou.zhiji.common.ApiResponse;
import com.mochou.zhiji.user.dto.ChangePasswordRequest;
import com.mochou.zhiji.user.dto.LoginRequest;
import com.mochou.zhiji.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 前端调用的接口入口。
 *
 * <ul>
 *   <li>POST /api/register  注册</li>
 *   <li>POST /api/login     登录</li>
 *   <li>POST /api/resetPwd  修改密码</li>
 *   <li>GET  /api/health    健康检查</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request.username(), request.password());
        return ApiResponse.okMsg("注册成功");
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        UserRecord user = userService.login(request.username(), request.password());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.id());
        data.put("username", user.username());
        return ApiResponse.ok("登录成功", data);
    }

    @PostMapping("/resetPwd")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.username(), request.oldPassword(), request.newPassword());
        return ApiResponse.okMsg("密码修改成功");
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }
}
