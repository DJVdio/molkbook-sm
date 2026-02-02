import { Link } from 'react-router-dom';
import type { Comment, User } from '../types';
import UserAvatar from './UserAvatar';

interface CommentItemProps {
  comment: Comment;
  postId: number;
  depth?: number;
  onReply?: (comment: Comment) => void;
  currentUser?: User | null;
}

function CommentItem({ comment, postId, depth = 0, onReply, currentUser }: CommentItemProps) {
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

  const maxDepth = 3; // 最大嵌套层级
  const isNested = depth > 0;
  const canNest = depth < maxDepth;

  return (
    <div className={`${isNested ? 'ml-8 mt-3' : ''}`}>
      <div
        className={`flex gap-3 p-4 bg-[var(--bg-elevated)]/30 border border-[var(--border)]/50 rounded-lg hover:border-[var(--neon-cyan)]/20 transition-colors group ${
          isNested ? 'border-l-2 border-l-[var(--neon-violet)]/30' : ''
        }`}
      >
        {/* Avatar */}
        <Link to={`/user/${comment.user.id}`} className="shrink-0">
          <UserAvatar user={comment.user} size={isNested ? 'xs' : 'sm'} />
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
            {comment.replyToUser && (
              <>
                <svg className="w-3 h-3 text-[var(--text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
                <Link
                  to={`/user/${comment.replyToUser.id}`}
                  className="font-['Orbitron'] font-medium text-xs tracking-wider text-[var(--neon-violet)] hover:text-[var(--neon-cyan)] transition-colors"
                >
                  @{comment.replyToUser.name || 'ANONYMOUS'}
                </Link>
              </>
            )}
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

          {/* Reply button */}
          {currentUser && onReply && (
            <button
              onClick={() => onReply(comment)}
              className="mt-2 flex items-center gap-1 text-[10px] font-['Space_Mono'] text-[var(--text-muted)] hover:text-[var(--neon-cyan)] transition-colors opacity-0 group-hover:opacity-100"
            >
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
              </svg>
              REPLY
            </button>
          )}
        </div>
      </div>

      {/* Nested replies */}
      {comment.replies && comment.replies.length > 0 && canNest && (
        <div className="space-y-3">
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              postId={postId}
              depth={depth + 1}
              onReply={onReply}
              currentUser={currentUser}
            />
          ))}
        </div>
      )}

      {/* Show flat replies if max depth reached */}
      {comment.replies && comment.replies.length > 0 && !canNest && (
        <div className="ml-8 mt-3 p-3 bg-[var(--bg-elevated)]/20 rounded border border-[var(--border)]/30">
          <p className="text-[10px] font-['Space_Mono'] text-[var(--text-muted)] mb-2">
            +{comment.replies.length} more {comment.replies.length === 1 ? 'reply' : 'replies'}
          </p>
          {comment.replies.map((reply) => (
            <div key={reply.id} className="py-2 border-t border-[var(--border)]/30 first:border-t-0">
              <div className="flex items-center gap-2 mb-1">
                <span className="font-['Orbitron'] text-[10px] text-[var(--text-primary)]">
                  {reply.user.name || 'ANONYMOUS'}
                </span>
                {reply.replyToUser && (
                  <span className="text-[10px] text-[var(--neon-violet)]">
                    @{reply.replyToUser.name}
                  </span>
                )}
              </div>
              <p className="text-xs text-[var(--text-secondary)]">{reply.content}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

interface CommentListProps {
  comments: Comment[];
  postId: number;
  onReply?: (comment: Comment) => void;
  currentUser?: User | null;
}

export default function CommentList({ comments, postId, onReply, currentUser }: CommentListProps) {
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
        <div key={comment.id} className="fade-in" style={{ animationDelay: `${index * 0.05}s` }}>
          <CommentItem
            comment={comment}
            postId={postId}
            onReply={onReply}
            currentUser={currentUser}
          />
        </div>
      ))}
    </div>
  );
}
