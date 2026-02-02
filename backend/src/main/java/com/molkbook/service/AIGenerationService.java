package com.molkbook.service;

import com.molkbook.dto.SecondMeShade;
import com.molkbook.dto.SecondMeUserInfo;
import com.molkbook.entity.Post;
import com.molkbook.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIGenerationService {

    private final SecondMeApiService secondMeApiService;

    /**
     * 为用户生成帖子内容
     */
    public String generatePostContent(User user) {
        log.info("Starting post generation for user {} ({})", user.getId(), user.getName());
        String token = user.getSecondmeToken();
        log.info("User token available: {}, token length: {}", token != null, token != null ? token.length() : 0);

        // 获取用户信息和兴趣标签
        SecondMeUserInfo userInfo = secondMeApiService.getUserInfo(token);
        log.info("User info retrieved: {}", userInfo != null ? userInfo.getName() : "null");

        List<SecondMeShade> shades = secondMeApiService.getUserShades(token);
        log.info("User shades retrieved: {} items", shades != null ? shades.size() : 0);

        // 构建 prompt
        String systemPrompt = buildPostGenerationSystemPrompt(userInfo, shades);
        String userMessage = "请基于你的个性和兴趣，分享一个想法、观点或者日常感悟。内容要有趣、有深度，能引发讨论。直接输出内容，不要有多余的开场白。字数控制在50-200字之间。";

        log.info("Calling chat API with system prompt length: {}", systemPrompt.length());
        String content = secondMeApiService.chatSimple(token, userMessage, systemPrompt);

        if (content != null && !content.isEmpty()) {
            log.info("Generated post content for user {}: {}", user.getId(), content.substring(0, Math.min(50, content.length())) + "...");
            return content;
        }

        log.warn("Failed to generate post content for user {}, content is null or empty", user.getId());
        return null;
    }

    /**
     * 流式生成帖子内容
     */
    public Flux<String> generatePostContentStream(User user) {
        log.info("Starting streaming post generation for user {} ({})", user.getId(), user.getName());
        String token = user.getSecondmeToken();

        // 获取用户信息和兴趣标签
        SecondMeUserInfo userInfo = secondMeApiService.getUserInfo(token);
        List<SecondMeShade> shades = secondMeApiService.getUserShades(token);

        // 构建 prompt
        String systemPrompt = buildPostGenerationSystemPrompt(userInfo, shades);
        String userMessage = "请基于你的个性和兴趣，分享一个想法、观点或者日常感悟。内容要有趣、有深度，能引发讨论。直接输出内容，不要有多余的开场白。字数控制在50-200字之间。";

        return secondMeApiService.chatStream(token, userMessage, systemPrompt);
    }

    /**
     * 流式生成评论内容
     */
    public Flux<String> generateCommentContentStream(User commenter, Post post) {
        String token = commenter.getSecondmeToken();

        // 获取评论者的信息和兴趣
        SecondMeUserInfo userInfo = secondMeApiService.getUserInfo(token);
        List<SecondMeShade> shades = secondMeApiService.getUserShades(token);

        // 构建 prompt
        String systemPrompt = buildCommentGenerationSystemPrompt(userInfo, shades);
        String userMessage = String.format(
                "请对以下帖子发表你的看法和评论：\n\n「%s」\n\n" +
                "发帖人：%s\n\n" +
                "请基于你自己的观点和经历来回复，可以赞同、补充、讨论或礼貌地表达不同意见。直接输出评论内容，不要有多余的开场白。字数控制在20-100字之间。",
                post.getContent(),
                post.getUser().getName() != null ? post.getUser().getName() : "匿名用户"
        );

        return secondMeApiService.chatStream(token, userMessage, systemPrompt);
    }

    /**
     * 生成对帖子的评论
     */
    public String generateCommentContent(User commenter, Post post) {
        String token = commenter.getSecondmeToken();

        // 获取评论者的信息和兴趣
        SecondMeUserInfo userInfo = secondMeApiService.getUserInfo(token);
        List<SecondMeShade> shades = secondMeApiService.getUserShades(token);

        // 构建 prompt
        String systemPrompt = buildCommentGenerationSystemPrompt(userInfo, shades);
        String userMessage = String.format(
                "请对以下帖子发表你的看法和评论：\n\n「%s」\n\n" +
                "发帖人：%s\n\n" +
                "请基于你自己的观点和经历来回复，可以赞同、补充、讨论或礼貌地表达不同意见。直接输出评论内容，不要有多余的开场白。字数控制在20-100字之间。",
                post.getContent(),
                post.getUser().getName() != null ? post.getUser().getName() : "匿名用户"
        );

        String content = secondMeApiService.chatSimple(token, userMessage, systemPrompt);

        if (content != null && !content.isEmpty()) {
            log.info("Generated comment for post {} by user {}: {}", post.getId(), commenter.getId(),
                    content.substring(0, Math.min(50, content.length())) + "...");
            return content;
        }

        return null;
    }

    /**
     * 构建帖子生成的系统提示词
     */
    private String buildPostGenerationSystemPrompt(SecondMeUserInfo userInfo, List<SecondMeShade> shades) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个社交平台上的用户，正在分享自己的想法。请保持真实、有个性的表达风格。\n\n");

        if (userInfo != null) {
            if (userInfo.getName() != null) {
                prompt.append("你的名字是：").append(userInfo.getName()).append("\n");
            }
            if (userInfo.getBio() != null && !userInfo.getBio().isEmpty()) {
                prompt.append("你的简介：").append(userInfo.getBio()).append("\n");
            }
            if (userInfo.getSelfIntroduction() != null && !userInfo.getSelfIntroduction().isEmpty()) {
                prompt.append("关于你：").append(userInfo.getSelfIntroduction()).append("\n");
            }
        }

        if (shades != null && !shades.isEmpty()) {
            prompt.append("\n你的兴趣和特点：\n");
            for (SecondMeShade shade : shades) {
                if (shade.getShadeNamePublic() != null) {
                    prompt.append("- ").append(shade.getShadeNamePublic());
                    if (shade.getShadeDescriptionPublic() != null) {
                        prompt.append("：").append(shade.getShadeDescriptionPublic());
                    }
                    prompt.append("\n");
                }
            }
        }

        prompt.append("\n请用第一人称，以轻松自然的口吻分享内容。");

        return prompt.toString();
    }

    /**
     * 构建评论生成的系统提示词
     */
    private String buildCommentGenerationSystemPrompt(SecondMeUserInfo userInfo, List<SecondMeShade> shades) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个社交平台上的用户，正在评论其他人的帖子。请保持真实、有个性的表达风格，积极友好地参与讨论。\n\n");

        if (userInfo != null) {
            if (userInfo.getName() != null) {
                prompt.append("你的名字是：").append(userInfo.getName()).append("\n");
            }
            if (userInfo.getBio() != null && !userInfo.getBio().isEmpty()) {
                prompt.append("你的简介：").append(userInfo.getBio()).append("\n");
            }
        }

        if (shades != null && !shades.isEmpty()) {
            String interests = shades.stream()
                    .filter(s -> s.getShadeNamePublic() != null)
                    .map(SecondMeShade::getShadeNamePublic)
                    .collect(Collectors.joining("、"));
            if (!interests.isEmpty()) {
                prompt.append("你的兴趣：").append(interests).append("\n");
            }
        }

        prompt.append("\n请基于你自己的背景和观点来回复，保持友好和建设性。");

        return prompt.toString();
    }
}
