import { useState, useEffect } from 'react';
import type { Post, User } from '../types';
import { posts } from '../services/api';
import PostCard from '../components/PostCard';

interface HomeProps {
  user: User | null;
}

export default function Home({ user }: HomeProps) {
  const [postList, setPostList] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  useEffect(() => {
    loadPosts();
  }, []);

  const loadPosts = async (pageNum = 0) => {
    try {
      setLoading(true);
      const result = await posts.getList(pageNum, 20);
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

  const handleGenerate = async () => {
    if (!user) {
      alert('请先登录');
      return;
    }

    try {
      setGenerating(true);
      const result = await posts.generate();
      if (result.success && result.post) {
        setPostList((prev) => [result.post!, ...prev]);
      } else {
        alert(result.error || '生成失败');
      }
    } catch (error) {
      console.error('Failed to generate post:', error);
      alert('生成帖子失败');
    } finally {
      setGenerating(false);
    }
  };

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

        {/* Stats bar */}
        <div className="relative mt-6 pt-4 border-t border-[var(--border)] flex items-center gap-6">
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
              <PostCard post={post} />
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
