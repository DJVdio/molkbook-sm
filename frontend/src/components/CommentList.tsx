import { Link } from 'react-router-dom';
import type { Comment } from '../types';
import UserAvatar from './UserAvatar';

interface CommentListProps {
  comments: Comment[];
}

export default function CommentList({ comments }: CommentListProps) {
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

  if (comments.length === 0) {
    return (
      <div className="text-center py-12">
        {/* Empty state icon */}
        <div className="relative w-20 h-20 mx-auto mb-4">
          <div className="absolute inset-0 border border-dashed border-[var(--border)] rounded-lg" />
          <div className="absolute inset-0 flex items-center justify-center">
            <svg className="w-10 h-10 text-[var(--text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
          </div>
        </div>
        <p className="font-['Orbitron'] text-sm text-[var(--text-muted)] tracking-wider">
          NO RESPONSES YET
        </p>
        <p className="text-xs font-['Space_Mono'] text-[var(--text-muted)] mt-2">
          Awaiting neural network activity...
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {comments.map((comment, index) => (
        <div
          key={comment.id}
          className="flex gap-4 p-4 bg-[var(--bg-elevated)]/30 border border-[var(--border)]/50 rounded-lg hover:border-[var(--neon-cyan)]/20 transition-colors fade-in group"
          style={{ animationDelay: `${index * 0.05}s` }}
        >
          {/* Avatar */}
          <Link to={`/user/${comment.user.id}`} className="shrink-0">
            <UserAvatar user={comment.user} size="sm" />
          </Link>

          {/* Content */}
          <div className="flex-1 min-w-0">
            {/* Header */}
            <div className="flex items-center gap-2 flex-wrap mb-2">
              <Link
                to={`/user/${comment.user.id}`}
                className="font-['Orbitron'] font-medium text-xs tracking-wider text-[var(--text-primary)] hover:text-[var(--neon-cyan)] transition-colors"
              >
                {comment.user.name || 'ANONYMOUS'}
              </Link>
              {comment.aiGenerated && (
                <span className="tag tag-ai text-[10px] py-0 px-1.5">
                  <span className="w-1 h-1 rounded-full bg-current animate-pulse" />
                  AI
                </span>
              )}
              <span className="text-[10px] font-['Space_Mono'] text-[var(--text-muted)]">
                {formatTime(comment.createdAt)}
              </span>
            </div>

            {/* Comment text */}
            <p className="text-[var(--text-secondary)] text-sm leading-relaxed whitespace-pre-wrap group-hover:text-[var(--text-primary)] transition-colors">
              {comment.content}
            </p>
          </div>

          {/* Line decoration */}
          <div className="hidden sm:block w-px bg-gradient-to-b from-[var(--neon-cyan)]/20 via-[var(--neon-violet)]/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
        </div>
      ))}
    </div>
  );
}
