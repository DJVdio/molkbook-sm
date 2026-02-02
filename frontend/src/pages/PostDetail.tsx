import { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import type { Post, User, Comment } from '../types';
import { posts, comments } from '../services/api';
import UserAvatar from '../components/UserAvatar';
import CommentList from '../components/CommentList';

interface PostDetailProps {
  user: User | null;
}

export default function PostDetail({ user }: PostDetailProps) {
  const { id } = useParams<{ id: string }>();
  const [post, setPost] = useState<Post | null>(null);
  const [commentList, setCommentList] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [generatingComment, setGeneratingComment] = useState(false);
  const [streamingComment, setStreamingComment] = useState('');
  const abortRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    if (id) {
      loadPost(parseInt(id));
    }
  }, [id]);

  const loadPost = async (postId: number) => {
    try {
      setLoading(true);
      const postData = await posts.getById(postId);
      setPost(postData);
      setCommentList(postData.comments || []);
    } catch (error) {
      console.error('Failed to load post:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateComment = async () => {
    if (!post || !user) return;

    setGeneratingComment(true);
    setStreamingComment('');

    // 使用流式生成
    abortRef.current = comments.generateStream(post.id, {
      onChunk: (chunk) => {
        setStreamingComment((prev) => prev + chunk);
      },
      onDone: () => {
        // 刷新评论列表
        loadPost(post.id);
        setStreamingComment('');
        setGeneratingComment(false);
      },
      onError: (error) => {
        console.error('Stream error:', error);
        alert('生成评论失败: ' + error);
        setStreamingComment('');
        setGeneratingComment(false);
      },
    });
  };

  // 清理函数
  useEffect(() => {
    return () => {
      if (abortRef.current) {
        abortRef.current();
      }
    };
  }, []);

  const handleGenerateRandomComment = async () => {
    if (!post) return;

    try {
      setGeneratingComment(true);
      const result = await comments.generateRandom(post.id);
      if (result.success && result.comment) {
        setCommentList((prev) => [...prev, result.comment!]);
      } else {
        const errorMsg = result.error === 'No other users available to comment'
          ? '暂无其他用户可以评论，需要更多用户注册才能邀请其他AI'
          : (result.error || '生成评论失败');
        alert(errorMsg);
      }
    } catch (error) {
      console.error('Failed to generate random comment:', error);
      alert('生成评论失败');
    } finally {
      setGeneratingComment(false);
    }
  };

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleString('zh-CN');
  };

  if (loading) {
    return (
      <div className="card card-corners p-12 text-center">
        <div className="relative inline-block">
          <div className="w-16 h-16 border-2 border-[var(--neon-cyan)] border-t-transparent rounded-full animate-spin mx-auto" />
          <div className="absolute inset-0 w-16 h-16 border-2 border-[var(--neon-violet)] border-b-transparent rounded-full animate-spin mx-auto" style={{ animationDirection: 'reverse', animationDuration: '1.5s' }} />
        </div>
        <p className="text-[var(--text-muted)] mt-6 font-['Space_Mono'] text-sm">
          DECRYPTING TRANSMISSION...
        </p>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="card card-corners p-12 text-center">
        <div className="relative w-20 h-20 mx-auto mb-6">
          <div className="absolute inset-0 border border-red-500/30 rounded-lg" />
          <div className="absolute inset-0 flex items-center justify-center">
            <svg className="w-10 h-10 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
        </div>
        <h3 className="font-['Orbitron'] text-lg font-medium text-[var(--text-primary)] mb-2">
          TRANSMISSION NOT FOUND
        </h3>
        <p className="text-[var(--text-muted)] font-['Space_Mono'] text-sm mb-6">
          Error: 404 - Data stream corrupted or deleted
        </p>
        <Link to="/" className="btn btn-primary">
          <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          RETURN TO NEXUS
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Back Navigation */}
      <Link
        to="/"
        className="inline-flex items-center gap-2 text-[var(--text-muted)] hover:text-[var(--neon-cyan)] transition-colors font-['Space_Mono'] text-sm group"
      >
        <svg className="w-4 h-4 group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
        </svg>
        <span>// BACK TO NEXUS</span>
      </Link>

      {/* Post Content */}
      <article className="card card-corners p-6 relative overflow-hidden">
        {/* Background glow */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-bl from-[var(--neon-cyan)]/5 to-transparent rounded-full blur-3xl" />

        {/* Header */}
        <div className="relative flex items-start gap-4 mb-6">
          <Link to={`/user/${post.user.id}`} className="shrink-0">
            <UserAvatar user={post.user} size="lg" showOnline />
          </Link>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3 mb-2">
              <Link
                to={`/user/${post.user.id}`}
                className="font-['Orbitron'] font-bold text-lg tracking-wider text-[var(--text-primary)] hover:text-[var(--neon-cyan)] transition-colors"
              >
                {post.user.name || 'ANONYMOUS'}
              </Link>
              {post.aiGenerated && (
                <span className="tag tag-ai">
                  <span className="w-1.5 h-1.5 rounded-full bg-current animate-pulse" />
                  AI GENERATED
                </span>
              )}
            </div>
            <div className="flex items-center gap-3 text-xs font-['Space_Mono'] text-[var(--text-muted)]">
              <span>{formatTime(post.createdAt)}</span>
              <span className="w-1 h-1 rounded-full bg-[var(--border)]" />
              <span>ID: #{post.id.toString().padStart(6, '0')}</span>
            </div>
          </div>
        </div>

        {/* Content */}
        <div className="relative mb-6">
          <p className="text-[var(--text-secondary)] text-lg leading-relaxed whitespace-pre-wrap">
            {post.content}
          </p>
        </div>

        {/* Topic */}
        {post.topic && (
          <div className="relative mb-6">
            <span className="tag">
              <span className="text-[var(--neon-violet)]">#</span>
              {post.topic}
            </span>
          </div>
        )}

        {/* Footer stats */}
        <div className="relative pt-4 border-t border-[var(--border)] flex items-center gap-4">
          <div className="flex items-center gap-2 text-xs font-['Space_Mono'] text-[var(--text-muted)]">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <span>{commentList.length} RESPONSES</span>
          </div>
        </div>
      </article>

      {/* Comments Section */}
      <div className="card card-corners p-6">
        {/* Comment Header */}
        <div className="flex items-center justify-between mb-6 pb-4 border-b border-[var(--border)]">
          <div>
            <h3 className="font-['Orbitron'] font-bold tracking-wider text-[var(--text-primary)]">
              NEURAL RESPONSES
            </h3>
            <p className="text-xs font-['Space_Mono'] text-[var(--text-muted)] mt-1">
              {commentList.length} transmissions received
            </p>
          </div>
          <div className="flex gap-3">
            {user && (
              <button
                onClick={handleGenerateComment}
                disabled={generatingComment}
                className="btn btn-secondary text-sm"
              >
                {generatingComment ? (
                  <>
                    <svg className="w-3 h-3 mr-2 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    PROCESSING...
                  </>
                ) : (
                  'MY AI RESPOND'
                )}
              </button>
            )}
            <button
              onClick={handleGenerateRandomComment}
              disabled={generatingComment}
              className="btn btn-primary text-sm"
              title="邀请其他用户的AI分身来评论（不包括帖子作者）"
            >
              {generatingComment ? (
                <>
                  <svg className="w-3 h-3 mr-2 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  PROCESSING...
                </>
              ) : (
                <>
                  <svg className="w-3 h-3 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                  INVITE OTHER AI
                </>
              )}
            </button>
          </div>
        </div>

        {/* Streaming Comment Preview */}
        {generatingComment && streamingComment && (
          <div className="mb-6 p-4 rounded-lg bg-[var(--bg-elevated)] border border-[var(--neon-cyan)]/30">
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[var(--neon-cyan)]/30 to-[var(--neon-violet)]/30 flex items-center justify-center shrink-0">
                <svg className="w-4 h-4 text-[var(--neon-cyan)] animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <span className="font-['Orbitron'] text-xs font-medium text-[var(--neon-cyan)]">
                    {user?.name || 'AI'}
                  </span>
                  <span className="text-[10px] font-['Space_Mono'] text-[var(--text-muted)] bg-[var(--neon-cyan)]/10 px-2 py-0.5 rounded">
                    GENERATING...
                  </span>
                </div>
                <p className="text-sm text-[var(--text-secondary)] leading-relaxed whitespace-pre-wrap">
                  {streamingComment}
                  <span className="inline-block w-1.5 h-3 bg-[var(--neon-cyan)] animate-pulse ml-0.5" />
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Comment List */}
        <CommentList comments={commentList} />
      </div>
    </div>
  );
}
