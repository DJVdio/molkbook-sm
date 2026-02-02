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

  like: (postId: number) =>
    request<{ success: boolean; message?: string }>(`/posts/${postId}/like`, {
      method: 'POST',
    }),

  unlike: (postId: number) =>
    request<{ success: boolean }>(`/posts/${postId}/like`, {
      method: 'DELETE',
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

  generateRandom: (postId: number) =>
    request<{ success: boolean; comment?: Comment; error?: string }>(
      `/posts/${postId}/comments/generate-random`,
      { method: 'POST' }
    ),
};
