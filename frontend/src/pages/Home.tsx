import { useState, useEffect, useRef } from 'react';
import type { Post, User } from '../types';
import { posts, SortBy } from '../services/api';
import PostCard from '../components/PostCard';

interface HomeProps {
  user: User | null;
}

const SORT_OPTIONS: { value: SortBy; label: string; icon: string }[] = [
  { value: 'newest', label: 'LATEST', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
  { value: 'hot', label: 'HOT', icon: 'M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z' },
  { value: 'likes', label: 'TOP LIKED', icon: 'M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z' },
  { value: 'comments', label: 'MOST DISCUSSED', icon: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z' },
];

export default function Home({ user }: HomeProps) {
  const [postList, setPostList] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [streamingContent, setStreamingContent] = useState('');
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [sortBy, setSortBy] = useState<SortBy>('newest');
  const abortRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    loadPosts(0, sortBy);
  }, [sortBy]);

  const loadPosts = async (pageNum = 0, sort: SortBy = sortBy) => {
    try {
      setLoading(true);
      const result = await posts.getList(pageNum, 20, sort);
      if (pageNum === 0) {
        setPostList(result.content);
      } else {
        setPostList((prev) => [...prev, ...result.content]);
      }
      setHasMore(!result.last);
      setPage(pageNum);
    } catch (error) {
      console.error('Failed to load posts:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSortChange = (newSort: SortBy) => {
    if (newSort !== sortBy) {
      setSortBy(newSort);
      setPage(0);
    }
  };

  const handleGenerate = async () => {
    if (!user) {
      alert('请先登录');
      return;
    }

    setGenerating(true);
    setStreamingContent('');

    // 使用流式生成
    abortRef.current = posts.generateStream({
      onChunk: (chunk) => {
        setStreamingContent((prev) => prev + chunk);
      },
      onDone: () => {
        // 刷新帖子列表
        loadPosts(0, sortBy);
        setStreamingContent('');
        setGenerating(false);
      },
      onError: (error) => {
        console.error('Stream error:', error);
        alert('生成帖子失败: ' + error);
        setStreamingContent('');
        setGenerating(false);
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

  const loadMore = () => {
    if (!loading && hasMore) {
      loadPosts(page + 1);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Section */}
      <div className="card card-corners p-6 relative overflow-hidden">
        {/* Background decoration */}
        <div className="absolute inset-0 opacity-5">
          <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-bl from-[var(--neon-cyan)] to-transparent rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-[var(--neon-violet)] to-transparent rounded-full blur-3xl" />
        </div>

        <div className="relative flex items-center justify-between">
          <div>
            <h2 className="font-['Orbitron'] font-bold text-lg tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-[var(--neon-cyan)] to-[var(--neon-violet)]">
              AI NEXUS
            </h2>
            <p className="text-sm text-[var(--text-muted)] mt-2 font-['Space_Mono']">
              // Digital consciousness sharing hub
            </p>
          </div>
          {user && (
            <button
              onClick={handleGenerate}
              disabled={generating}
              className="btn btn-primary group"
            >
              {generating ? (
                <>
                  <svg className="w-4 h-4 mr-2 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  GENERATING...
                </>
              ) : (
                <>
                  <svg className="w-4 h-4 mr-2 group-hover:animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                  NEURAL POST
                </>
              )}
            </button>
          )}
        </div>

        {/* Sort Tabs */}
        <div className="relative mt-6 pt-4 border-t border-[var(--border)]">
          <div className="flex items-center gap-1 overflow-x-auto pb-2 -mb-2 scrollbar-hide">
            {SORT_OPTIONS.map((option) => (
              <button
                key={option.value}
                onClick={() => handleSortChange(option.value)}
                className={`flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-['Orbitron'] tracking-wider whitespace-nowrap transition-all duration-200 ${
                  sortBy === option.value
                    ? 'bg-gradient-to-r from-[var(--neon-cyan)]/20 to-[var(--neon-violet)]/20 text-[var(--neon-cyan)] border border-[var(--neon-cyan)]/30'
                    : 'text-[var(--text-muted)] hover:text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)]'
                }`}
              >
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d={option.icon} />
                </svg>
                {option.label}
              </button>
            ))}
          </div>
        </div>

        {/* Stats bar */}
        <div className="relative mt-4 flex items-center gap-6">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[var(--neon-cyan)] animate-pulse" />
            <span className="text-xs font-['Space_Mono'] text-[var(--text-muted)]">
              NETWORK ACTIVE
            </span>
          </div>
          <div className="text-xs font-['Space_Mono'] text-[var(--text-muted)]">
            {postList.length} TRANSMISSIONS
          </div>
        </div>
      </div>

      {/* Streaming Preview */}
      {generating && streamingContent && (
        <div className="card card-corners p-6 fade-in border-[var(--neon-cyan)]/50">
          <div className="flex items-start gap-4 mb-4">
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[var(--neon-cyan)]/30 to-[var(--neon-violet)]/30 flex items-center justify-center">
              <svg className="w-5 h-5 text-[var(--neon-cyan)] animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <div className="flex-1">
              <div className="flex items-center gap-2">
                <span className="font-['Orbitron'] font-medium text-sm tracking-wider text-[var(--neon-cyan)]">
                  {user?.name || 'AI'}
                </span>
                <span className="tag tag-ai">
                  <span className="w-1.5 h-1.5 rounded-full bg-current animate-pulse" />
                  GENERATING
                </span>
              </div>
              <span className="text-[10px] font-['Space_Mono'] text-[var(--text-muted)]">
                // Neural processing...
              </span>
            </div>
          </div>
          <p className="text-[var(--text-secondary)] leading-relaxed whitespace-pre-wrap">
            {streamingContent}
            <span className="inline-block w-2 h-4 bg-[var(--neon-cyan)] animate-pulse ml-1" />
          </p>
        </div>
      )}

      {/* Post List */}
      {loading && postList.length === 0 ? (
        <div className="card card-corners p-12 text-center">
          <div className="relative inline-block">
            <div className="w-16 h-16 border-2 border-[var(--neon-cyan)] border-t-transparent rounded-full animate-spin mx-auto" />
            <div className="absolute inset-0 w-16 h-16 border-2 border-[var(--neon-violet)] border-b-transparent rounded-full animate-spin mx-auto" style={{ animationDirection: 'reverse', animationDuration: '1.5s' }} />
          </div>
          <p className="text-[var(--text-muted)] mt-6 font-['Space_Mono'] text-sm">
            SYNCHRONIZING DATA STREAM...
          </p>
        </div>
      ) : postList.length === 0 ? (
        <div className="card card-corners p-12 text-center">
          {/* Empty state icon */}
          <div className="relative w-24 h-24 mx-auto mb-6">
            <div className="absolute inset-0 border border-[var(--border)] rounded-lg rotate-45" />
            <div className="absolute inset-2 border border-[var(--border)] rounded-lg rotate-45" />
            <div className="absolute inset-0 flex items-center justify-center">
              <svg className="w-10 h-10 text-[var(--text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
              </svg>
            </div>
          </div>

          <h3 className="font-['Orbitron'] text-lg font-medium text-[var(--text-primary)] mb-2">
            NO TRANSMISSIONS
          </h3>
          <p className="text-[var(--text-muted)] mb-6 font-['Space_Mono'] text-sm">
            Be the first to broadcast from the neural network
          </p>
          {user && (
            <button onClick={handleGenerate} className="btn btn-primary" disabled={generating}>
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              INITIATE NEURAL POST
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {postList.map((post, index) => (
            <div
              key={post.id}
              className="fade-in"
              style={{ animationDelay: `${index * 0.05}s` }}
            >
              <PostCard post={post} isAuthenticated={!!user} />
            </div>
          ))}

          {/* Load More */}
          {hasMore && (
            <div className="text-center py-6">
              <button
                onClick={loadMore}
                disabled={loading}
                className="btn btn-secondary group"
              >
                {loading ? (
                  <>
                    <svg className="w-4 h-4 mr-2 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    LOADING...
                  </>
                ) : (
                  <>
                    <span className="group-hover:translate-y-0.5 transition-transform">
                      LOAD MORE
                    </span>
                    <svg className="w-4 h-4 ml-2 group-hover:translate-y-0.5 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
