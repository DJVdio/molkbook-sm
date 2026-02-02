package com.molkbook.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molkbook.dto.SecondMeApiResponse;
import com.molkbook.dto.SecondMeShade;
import com.molkbook.dto.SecondMeUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class SecondMeApiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${secondme.api.base-url}")
    private String baseUrl;

    @Value("${secondme.api.oauth.client-id}")
    private String clientId;

    @Value("${secondme.api.oauth.client-secret}")
    private String clientSecret;

    @Value("${secondme.api.oauth.redirect-uri}")
    private String redirectUri;

    public SecondMeApiService(ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .filter((request, next) -> next.exchange(request)
                        .flatMap(response -> {
                            if (response.statusCode().isError()) {
                                return response.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            log.error("HTTP error {}: {}", response.statusCode(), body);
                                            return reactor.core.publisher.Mono.error(
                                                    new RuntimeException("HTTP error " + response.statusCode() + ": " + body));
                                        });
                            }
                            return reactor.core.publisher.Mono.just(response);
                        }))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 用授权码换取 access token
     */
    public String exchangeCodeForToken(String code) {
        try {
            String formBody = "grant_type=authorization_code" +
                    "&code=" + code +
                    "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8) +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret;

            String response = webClient.post()
                    .uri(baseUrl + "/api/oauth/token/code")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Token exchange response: {}", response);

            JsonNode root = objectMapper.readTree(response);
            if (root.has("code") && root.get("code").asInt() == 0) {
                JsonNode data = root.get("data");
                if (data != null && data.has("accessToken")) {
                    return data.get("accessToken").asText();
                }
            }
            log.error("Failed to exchange code for token: {}", response);
            return null;
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            return null;
        }
    }

    /**
     * 获取用户信息
     */
    public SecondMeUserInfo getUserInfo(String token) {
        try {
            log.info("Getting user info from SecondMe API, token length: {}", token != null ? token.length() : 0);
            String response = webClient.get()
                    .uri(baseUrl + "/api/secondme/user/info")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("SecondMe user info response: {}", response);
            JsonNode root = objectMapper.readTree(response);
            if (root.has("code") && root.get("code").asInt() == 0) {
                SecondMeUserInfo userInfo = objectMapper.treeToValue(root.get("data"), SecondMeUserInfo.class);
                log.info("Successfully parsed user info: {}", userInfo.getName());
                return userInfo;
            }
            log.error("Failed to get user info, response code is not 0: {}", response);
            return null;
        } catch (Exception e) {
            log.error("Error getting user info", e);
            return null;
        }
    }

    /**
     * 获取用户兴趣标签
     */
    public List<SecondMeShade> getUserShades(String token) {
        try {
            String response = webClient.get()
                    .uri(baseUrl + "/api/secondme/user/shades")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            if (root.has("code") && root.get("code").asInt() == 0) {
                JsonNode dataNode = root.get("data");
                if (dataNode != null && dataNode.has("shades")) {
                    JsonNode shadesNode = dataNode.get("shades");
                    return objectMapper.readValue(
                            shadesNode.toString(),
                            new TypeReference<List<SecondMeShade>>() {}
                    );
                }
                log.warn("User shades data is missing or empty");
                return List.of();
            }
            log.error("Failed to get user shades, response code is not 0: {}", response);
            return List.of();
        } catch (Exception e) {
            log.error("Error getting user shades", e);
            return List.of();
        }
    }

    /**
     * 流式聊天 - 用于 AI 生成内容
     */
    public String chat(String token, String message, String systemPrompt) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("message", message);
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                body.put("systemPrompt", systemPrompt);
            }

            StringBuilder result = new StringBuilder();

            Flux<String> responseFlux = webClient.post()
                    .uri(baseUrl + "/api/secondme/chat/stream")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToFlux(String.class);

            responseFlux.toStream().forEach(line -> {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if (!"[DONE]".equals(data)) {
                        try {
                            JsonNode node = objectMapper.readTree(data);
                            if (node.has("choices")) {
                                JsonNode choices = node.get("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).get("delta");
                                    if (delta != null && delta.has("content")) {
                                        result.append(delta.get("content").asText());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignore parsing errors for individual chunks
                        }
                    }
                }
            });

            return result.toString();
        } catch (Exception e) {
            log.error("Error in chat", e);
            return null;
        }
    }

    /**
     * 流式聊天 - 返回 Flux 用于 SSE 推送到前端
     */
    public Flux<String> chatStream(String token, String message, String systemPrompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("systemPrompt", systemPrompt);
        }

        return webClient.post()
                .uri(baseUrl + "/api/secondme/chat/stream")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(line -> {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if (!"[DONE]".equals(data) && !data.isEmpty()) {
                            try {
                                JsonNode node = objectMapper.readTree(data);
                                if (node.has("choices")) {
                                    JsonNode choices = node.get("choices");
                                    if (choices.isArray() && choices.size() > 0) {
                                        JsonNode delta = choices.get(0).get("delta");
                                        if (delta != null && delta.has("content")) {
                                            String content = delta.get("content").asText();
                                            if (!content.isEmpty()) {
                                                return Flux.just(content);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("Error parsing chunk: {}", data);
                            }
                        }
                    }
                    return Flux.empty();
                })
                .onErrorResume(e -> {
                    log.error("Error in chatStream", e);
                    return Flux.empty();
                });
    }

    /**
     * 非流式聊天 - 简化版本，直接获取完整响应
     */
    public String chatSimple(String token, String message, String systemPrompt) {
        try {
            log.info("Calling SecondMe chat API, message length: {}, systemPrompt length: {}",
                    message != null ? message.length() : 0,
                    systemPrompt != null ? systemPrompt.length() : 0);

            Map<String, Object> body = new HashMap<>();
            body.put("message", message);
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                body.put("systemPrompt", systemPrompt);
            }

            // 使用 SSE 流式响应，收集所有内容
            StringBuilder content = new StringBuilder();

            String response = webClient.post()
                    .uri(baseUrl + "/api/secondme/chat/stream")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("SecondMe chat response (first 500 chars): {}",
                    response != null && !response.isEmpty() ? response.substring(0, Math.min(500, response.length())) : "null");

            if (response != null) {
                String[] lines = response.split("\n");
                for (String line : lines) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if (!"[DONE]".equals(data) && !data.isEmpty()) {
                            try {
                                JsonNode node = objectMapper.readTree(data);
                                if (node.has("choices")) {
                                    JsonNode choices = node.get("choices");
                                    if (choices.isArray() && choices.size() > 0) {
                                        JsonNode delta = choices.get(0).get("delta");
                                        if (delta != null && delta.has("content")) {
                                            content.append(delta.get("content").asText());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("Error parsing chunk: {}", data);
                            }
                        }
                    }
                }
            }

            String result = content.toString();
            log.info("Chat result length: {}, content preview: {}",
                    result.length(),
                    result.isEmpty() ? "(empty)" : result.substring(0, Math.min(100, result.length())));
            return result;
        } catch (Exception e) {
            log.error("Error in chatSimple", e);
            return null;
        }
    }
}
