package com.example.mall.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mall.common.auth.LoginUser;
import com.example.mall.common.exception.BusinessException;
import com.example.mall.common.exception.ErrorCode;
import com.example.mall.module.auth.dto.AuthTokenResponse;
import com.example.mall.module.auth.dto.AuthUserResponse;
import com.example.mall.module.auth.dto.LoginRequest;
import com.example.mall.module.auth.dto.RefreshRequest;
import com.example.mall.module.auth.dto.RegisterRequest;
import com.example.mall.module.auth.service.AuthService;
import com.example.mall.module.merchant.entity.MerchantStaff;
import com.example.mall.module.merchant.mapper.MerchantStaffMapper;
import com.example.mall.module.user.entity.SysRole;
import com.example.mall.module.user.entity.SysUser;
import com.example.mall.module.user.entity.SysUserRole;
import com.example.mall.module.user.mapper.SysRoleMapper;
import com.example.mall.module.user.mapper.SysUserMapper;
import com.example.mall.module.user.mapper.SysUserRoleMapper;
import com.example.mall.security.JwtTokenService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final MerchantStaffMapper merchantStaffMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(
        SysUserMapper sysUserMapper,
        SysRoleMapper sysRoleMapper,
        SysUserRoleMapper sysUserRoleMapper,
        MerchantStaffMapper merchantStaffMapper,
        PasswordEncoder passwordEncoder,
        JwtTokenService jwtTokenService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.merchantStaffMapper = merchantStaffMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public AuthTokenResponse login(LoginRequest request) {
        SysUser user = findByUsername(request.username());
        if (user == null || !passwordMatches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "Invalid username or password");
        }
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthUserResponse register(RegisterRequest request) {
        if (findByUsername(request.username()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "Username already exists");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setStatus("ACTIVE");
        sysUserMapper.insert(user);

        Long consumerRoleId = resolveRoleId("CONSUMER");
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(consumerRoleId);
        sysUserRoleMapper.insert(userRole);
        return toResponse(user, Set.of("CONSUMER"), null);
    }

    @Override
    public AuthTokenResponse refresh(RefreshRequest request) {
        JwtTokenService.JwtClaims claims = jwtTokenService.parseAndValidate(request.refreshToken());
        if (!"refresh".equals(claims.get("type"))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "Invalid refresh token");
        }
        LoginUser loginUser = jwtTokenService.toLoginUser(claims);
        SysUser user = findById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "User not found");
        }
        return issueTokens(user);
    }

    @Override
    public AuthUserResponse currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
            ? null
            : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return new AuthUserResponse(loginUser.getUserId(), loginUser.getUsername(), loginUser.getNickname(), loginUser.getMerchantId(), loginUser.getRoles());
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "Unauthorized");
    }

    private AuthTokenResponse issueTokens(SysUser user) {
        Set<String> roles = resolveRoles(user.getId());
        Long merchantId = resolveMerchantId(user.getId(), roles);
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getPasswordHash(), user.getNickname(), merchantId, roles);
        String accessToken = jwtTokenService.createAccessToken(loginUser);
        String refreshToken = jwtTokenService.createRefreshToken(loginUser);
        return new AuthTokenResponse(
            accessToken,
            jwtTokenService.getAccessTokenTtlSeconds(),
            refreshToken,
            jwtTokenService.getRefreshTokenTtlSeconds(),
            toResponse(user, roles, merchantId)
        );
    }

    private AuthUserResponse toResponse(SysUser user, Set<String> roles, Long merchantId) {
        return new AuthUserResponse(user.getId(), user.getUsername(), user.getNickname(), merchantId, roles);
    }

    private SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).last("limit 1"));
    }

    private SysUser findById(Long id) {
        return sysUserMapper.selectById(id);
    }

    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        if (!encodedPassword.startsWith("{")) {
            return rawPassword.equals(encodedPassword);
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private Long resolveRoleId(String roleCode) {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode).last("limit 1"));
        if (role == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "Role not found: " + roleCode);
        }
        return role.getId();
    }

    private Set<String> resolveRoles(Long userId) {
        List<SysUserRole> relations = sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        if (relations.isEmpty()) {
            return Set.of("CONSUMER");
        }
        Set<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        Set<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toCollection(LinkedHashSet::new));
        return roleCodes.isEmpty() ? Set.of("CONSUMER") : roleCodes;
    }

    private Long resolveMerchantId(Long userId, Set<String> roles) {
        if (!roles.contains("MERCHANT_ADMIN") && !roles.contains("CUSTOMER_SERVICE")) {
            return null;
        }
        MerchantStaff staff = merchantStaffMapper.selectOne(
            new LambdaQueryWrapper<MerchantStaff>().eq(MerchantStaff::getUserId, userId).last("limit 1")
        );
        return staff == null ? null : staff.getMerchantId();
    }
}
