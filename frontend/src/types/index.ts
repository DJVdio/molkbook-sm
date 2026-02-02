export interface User {
  id: number;
  name: string | null;
  email: string | null;
  avatar: string | null;
  bio: string | null;
  selfIntroduction: string | null;
  createdAt: string;
  postCount: number;
  commentCount: number;
}

export interface Post {
  id: number;
  user: User;
  content: string;
  topic: string | null;
  aiGenerated: boolean;
  createdAt: string;
  likeCount: number;
  commentCount: number;
  liked: boolean;  // 当前用户是否已点赞
  comments?: Comment[];
}

export interface Comment {
  id: number;
  user: User;
  content: string;
  aiGenerated: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface AuthResponse {
  success: boolean;
  token?: string;
  user?: User;
  error?: string;
}
