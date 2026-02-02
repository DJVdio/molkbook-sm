import { Link } from 'react-router-dom';
import type { Post } from '../types';
import UserAvatar from './UserAvatar';

interface PostCardProps {
  post: Post;
}

export default function PostCard({ post }: PostCardProps) {

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();

    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'NOW';
    if (minutes < 60) return `${minutes}m`;
    if (hours < 24) return `${hours}h`;
    if (days < 7) return `${days}d`;
    return date.toLocaleDateString('zh-CN');
  };

  return (
    <article className="card card-corners p-6 fade-in group">
      {/* Header */}
      <div className="flex items-start gap-4 mb-4">
        <Link to={`/user/${post.user.id}`} className="shrink-0">
          <UserAvatar user={post.user} size="md" showOnline />
        </Link>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <Link
              to={`/user/${post.user.id}`}
              className="font-['Orbitron'] font-medium text-sm tracking-wider text-[var(--text-primary)] hover:text-[var(--neon-cyan)] transition-colors"
            >
              {post.user.name || 'ANONYMOUS'}
            </Link>
            {post.aiGenerated && (
              <span className="tag tag-ai">
                <span className="w-1.5 h-1.5 rounded-full bg-current animate-pulse" />
                AI
              </span>
            )}
          </div>
          <div className="flex items-center gap-2 mt-1">
            <span className="text-[10px] font-['Space_Mono'] text-[var(--text-muted)]">
              {formatTime(post.createdAt)}
            </span>
            <span className="w-1 h-1 rounded-full bg-[var(--border)]" />
            <span className="text-[10px] font-['Space_Mono'] text-[var(--text-muted)]">
              #{post.id.toString().padStart(6, '0')}
            </span>
          </div>
        </div>
      </div>

      {/* Content */}
      <Link to={`/post/${post.id}`} className="block">
        <p className="text-[var(--text-secondary)] leading-relaxed whitespace-pre-wrap group-hover:text-[var(--text-primary)] transition-colors">
          {post.content}
        </p>
      </Link>

      {/* Topic tag */}
      {post.topic && (
        <div className="mt-4">
          <span className="tag">
            <span className="text-[var(--neon-violet)]">#</span>
            {post.topic}
          </span>
        </div>
      )}

      {/* Footer */}
      <div className="mt-5 pt-4 border-t border-[var(--border)] flex items-center justify-between">
        <div className="flex items-center gap-4">
          {/* AI Likes display - 仅显示 AI 点赞数 */}
          <div
            className="flex items-center gap-2 text-xs font-['Orbitron'] tracking-wider text-[var(--neon-pink)]"
            title="AI 分身点赞数"
          >
            <svg
              className="w-4 h-4"
              fill={(post.likeCount || 0) > 0 ? 'currentColor' : 'none'}
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
              />
            </svg>
            <span>{post.likeCount || 0}</span>
            {(post.likeCount || 0) > 0 && <span className="text-[10px] opacity-70">AI</span>}
          </div>

          {/* Comments */}
          <Link
            to={`/post/${post.id}`}
            className="flex items-center gap-2 text-xs font-['Orbitron'] tracking-wider text-[var(--text-muted)] hover:text-[var(--neon-cyan)] transition-colors"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <span>{post.commentCount}</span>
          </Link>
        </div>

        <Link
          to={`/post/${post.id}`}
          className="flex items-center gap-1 text-xs font-['Orbitron'] tracking-wider text-[var(--text-muted)] hover:text-[var(--neon-cyan)] transition-colors"
        >
          <span>VIEW</span>
          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </Link>
      </div>
    </article>
  );
}
