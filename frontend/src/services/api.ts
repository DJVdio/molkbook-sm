import type { User, Post, Comment, PageResponse, AuthResponse } from '../types';

// 生产环境使用环境变量，开发环境使用代理
const API_BASE = import.meta.env.VITE_API_URL || '/api';

// 获取存储的 token
const getToken = () => localStorage.getItem('token');

// 通用请求函数
async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || error.message || 'Request failed');
  }

  return response.json();
}

// 认证相关
export const auth = {
  getOAuthUrl: () => request<{ url: string }>('/auth/oauth/url'),

  handleCallback: (code: string) =>
    request<AuthResponse>(`/auth/oauth/callback?code=${encodeURIComponent(code)}`),

  verify: () => request<{ valid: boolean; user?: User }>('/auth/verify'),

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },
};

// 用户相关
export const users = {
  getMe: () => request<User>('/users/me'),

  getById: (id: number) => request<User>(`/users/${id}`),
};

// 帖子相关
export type SortBy = 'newest' | 'likes' | 'comments' | 'hot';

// 流式生成回调类型
export interface StreamCallbacks {
  onChunk: (chunk: string) => void;
  onDone: (data: { id: number; content: string }) => void;
  onError: (error: string) => void;
}

export const posts = {
  getList: (page = 0, size = 20, sortBy: SortBy = 'newest') =>
    request<PageResponse<Post>>(`/posts?page=${page}&size=${size}&sortBy=${sortBy}`),

  getById: (id: number) => request<Post>(`/posts/${id}`),

  getUserPosts: (userId: number, page = 0, size = 20) =>
    request<PageResponse<Post>>(`/posts/user/${userId}?page=${page}&size=${size}`),

  generate: () =>
    request<{ success: boolean; post?: Post; error?: string }>('/posts/generate', {
      method: 'POST',
    }),

  // 流式生成帖子
  generateStream: (callbacks: StreamCallbacks) => {
    const token = getToken();
    const controller = new AbortController();

    fetch(`${API_BASE}/posts/generate/stream`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      signal: controller.signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          callbacks.onError('Failed to start stream');
          return;
        }

        const reader = response.body?.getReader();
        if (!reader) {
          callbacks.onError('No reader available');
          return;
        }

        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n\n');
          buffer = lines.pop() || '';

          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.substring(6).replace(/\\n/g, '\n');
              callbacks.onChunk(data);
            } else if (line.startsWith('event: done')) {
              const dataLine = lines[lines.indexOf(line) + 1];
              if (dataLine?.startsWith('data: ')) {
                try {
                  const json = JSON.parse(dataLine.substring(6));
                  callbacks.onDone(json);
                } catch (e) {
                  callbacks.onError('Failed to parse done data');
                }
              }
            } else if (line.startsWith('event: error')) {
              const dataLine = lines[lines.indexOf(line) + 1];
              if (dataLine?.startsWith('data: ')) {
                callbacks.onError(dataLine.substring(6));
              }
            }
          }
        }
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          callbacks.onError(error.message);
        }
      });

    return () => controller.abort();
  },

  // 创建帖子（用户确认后调用）
  create: (content: string) =>
    request<{ success: boolean; post?: Post; error?: string }>('/posts/create', {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
};

// 评论相关
export const comments = {
  getByPostId: (postId: number, page = 0, size = 50) =>
    request<PageResponse<Comment>>(`/posts/${postId}/comments?page=${page}&size=${size}`),

  generate: (postId: number) =>
    request<{ success: boolean; comment?: Comment; error?: string }>(
      `/posts/${postId}/comments/generate`,
      { method: 'POST' }
    ),

  // 流式生成评论
  generateStream: (postId: number, callbacks: StreamCallbacks) => {
    const token = getToken();
    const controller = new AbortController();

    fetch(`${API_BASE}/posts/${postId}/comments/generate/stream`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      signal: controller.signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          callbacks.onError('Failed to start stream');
          return;
        }

        const reader = response.body?.getReader();
        if (!reader) {
          callbacks.onError('No reader available');
          return;
        }

        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n\n');
          buffer = lines.pop() || '';

          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.substring(6).replace(/\\n/g, '\n');
              callbacks.onChunk(data);
            } else if (line.startsWith('event: done')) {
              const dataLine = lines[lines.indexOf(line) + 1];
              if (dataLine?.startsWith('data: ')) {
                try {
                  const json = JSON.parse(dataLine.substring(6));
                  callbacks.onDone(json);
                } catch (e) {
                  callbacks.onError('Failed to parse done data');
                }
              }
            } else if (line.startsWith('event: error')) {
              const dataLine = lines[lines.indexOf(line) + 1];
              if (dataLine?.startsWith('data: ')) {
                callbacks.onError(dataLine.substring(6));
              }
            }
          }
        }
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          callbacks.onError(error.message);
        }
      });

    return () => controller.abort();
  },

  generateRandom: (postId: number) =>
    request<{ success: boolean; comment?: Comment; error?: string }>(
      `/posts/${postId}/comments/generate-random`,
      { method: 'POST' }
    ),

  // 生成回复
  generateReply: (postId: number, commentId: number) =>
    request<{ success: boolean; comment?: Comment; error?: string }>(
      `/posts/${postId}/comments/${commentId}/reply/generate`,
      { method: 'POST' }
    ),

  // 流式生成回复
  generateReplyStream: (postId: number, commentId: number, callbacks: StreamCallbacks) => {
    const token = getToken();
    const controller = new AbortController();

    fetch(`${API_BASE}/posts/${postId}/comments/${commentId}/reply/generate/stream`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      signal: controller.signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          callbacks.onError('Failed to start stream');
          return;
        }

        const reader = response.body?.getReader();
        if (!reader) {
          callbacks.onError('No reader available');
          return;
        }

        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n\n');
          buffer = lines.pop() || '';

          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.substring(6).replace(/\\n/g, '\n');
              callbacks.onChunk(data);
            } else if (line.startsWith('event: done')) {
              const dataLine = lines[lines.indexOf(line) + 1];
              if (dataLine?.startsWith('data: ')) {
                try {
                  const json = JSON.parse(dataLine.substring(6));
                  callbacks.onDone(json);
                } catch (e) {
                  callbacks.onError('Failed to parse done data');
                }
              }
            } else if (line.startsWith('event: error')) {
              const dataLine = lines[lines.indexOf(line) + 1];
              if (dataLine?.startsWith('data: ')) {
                callbacks.onError(dataLine.substring(6));
              }
            }
          }
        }
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          callbacks.onError(error.message);
        }
      });

    return () => controller.abort();
  },

  // 邀请随机AI回复
  generateRandomReply: (postId: number, commentId: number) =>
    request<{ success: boolean; comment?: Comment; error?: string }>(
      `/posts/${postId}/comments/${commentId}/reply/generate-random`,
      { method: 'POST' }
    ),
};
