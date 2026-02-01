package com.molkbook.service;

import com.molkbook.dto.SecondMeShade;
import com.molkbook.dto.SecondMeUserInfo;
import com.molkbook.dto.UserDTO;
import com.molkbook.entity.User;
import com.molkbook.entity.UserShade;
import com.molkbook.repository.CommentRepository;
import com.molkbook.repository.PostRepository;
import com.molkbook.repository.UserRepository;
import com.molkbook.repository.UserShadeRepository;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserShadeRepository userShadeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final SecondMeApiService secondMeApiService;

    /**
     * 根据 SecondMe Token 创建或更新用户
     */
    @Transactional
    public User createOrUpdateUser(String secondmeToken) {
        // 获取 SecondMe 用户信息
        SecondMeUserInfo userInfo = secondMeApiService.getUserInfo(secondmeToken);
        if (userInfo == null) {
            throw new RuntimeException("Failed to get user info from SecondMe");
        }

        // 查找或创建用户
        Optional<User> existingUser = userRepository.findBySecondmeToken(secondmeToken);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = new User();
            user.setSecondmeToken(secondmeToken);
        }

        // 更新用户信息
        user.setName(userInfo.getName());
        user.setEmail(userInfo.getEmail());
        user.setAvatar(userInfo.getAvatar());
        user.setBio(userInfo.getBio());
        user.setSelfIntroduction(userInfo.getSelfIntroduction());

        user = userRepository.save(user);

        // 同步用户兴趣标签
        syncUserShades(user, secondmeToken);

        return user;
    }

    /**
     * 同步用户兴趣标签
     */
    @Transactional
    public void syncUserShades(User user, String token) {
        List<SecondMeShade> shades = secondMeApiService.getUserShades(token);

        // 删除旧的标签
        userShadeRepository.deleteByUserId(user.getId());

        // 添加新的标签
        for (SecondMeShade shade : shades) {
            if (shade.getHasPublicContent() != null && shade.getHasPublicContent()) {
                UserShade userShade = UserShade.builder()
                        .user(user)
                        .shadeName(shade.getShadeNamePublic() != null ? shade.getShadeNamePublic() : shade.getShadeName())
                        .shadeDescription(shade.getShadeDescriptionPublic() != null ? shade.getShadeDescriptionPublic() : shade.getShadeDescription())
                        .confidenceLevel(shade.getConfidenceLevelPublic() != null ? shade.getConfidenceLevelPublic() : shade.getConfidenceLevel())
                        .build();
                userShadeRepository.save(userShade);
            }
        }
    }

    /**
     * 根据 ID 获取用户
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 根据 SecondMe Token 获取用户
     */
    public Optional<User> findBySecondmeToken(String token) {
        return userRepository.findBySecondmeToken(token);
    }

    /**
     * 获取所有活跃用户
     */
    public List<User> findActiveUsers() {
        return userRepository.findActiveUsers();
    }

    /**
     * 获取随机用户（排除指定用户）
     */
    public List<User> findRandomUsersExcluding(Long excludeUserId) {
        return userRepository.findRandomUsersExcluding(excludeUserId);
    }

    /**
     * 转换为 DTO
     */
    public UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .selfIntroduction(user.getSelfIntroduction())
                .createdAt(user.getCreatedAt())
                .postCount(postRepository.countByUserId(user.getId()))
                .commentCount(commentRepository.countByUserId(user.getId()))
                .build();
    }
}
