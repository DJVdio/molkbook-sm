import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import type { Post, User } from '../types';
import { users, posts } from '../services/api';
import UserAvatar from '../components/UserAvatar';
import PostCard from '../components/PostCard';

interface ProfileProps {
  currentUser: User | null;
}

export default function Profile({ currentUser }: ProfileProps) {
  const { id } = useParams<{ id: string }>();
  const [user, setUser] = useState<User | null>(null);
  const [postList, setPostList] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);

  const userId = id ? parseInt(id) : currentUser?.id;

  useEffect(() => {
    if (userId) {
      loadUser(userId);
      loadPosts(userId);
    }
  }, [userId]);

  const loadUser = async (uid: number) => {
    try {
      const userData = await users.getById(uid);
      setUser(userData);
    } catch (error) {
      console.error('Failed to load user:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadPosts = async (uid: number) => {
    try {
      const result = await posts.getUserPosts(uid, 0, 20);
      setPostList(result.content);
    } catch (error) {
      console.error('Failed to load posts:', error);
    }
  };

  if (loading) {
    return (
      <div className="card card-corners p-12 text-center">
        <div className="relative inline-block">
          <div className="w-16 h-16 border-2 border-[var(--neon-cyan)] border-t-transparent rounded-full animate-spin mx-auto" />
          <div className="absolute inset-0 w-16 h-16 border-2 border-[var(--neon-violet)] border-b-transparent rounded-full animate-spin mx-auto" style={{ animationDirection: 'reverse', animationDuration: '1.5s' }} />
        </div>
        <p className="text-[var(--text-muted)] mt-6 font-['Space_Mono'] text-sm">
          LOADING PROFILE DATA...
        </p>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="card card-corners p-12 text-center">
        <div className="relative w-20 h-20 mx-auto mb-6">
          <div className="absolute inset-0 border border-red-500/30 rounded-lg" />
          <div className="absolute inset-0 flex items-center justify-center">
            <svg className="w-10 h-10 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </div>
        </div>
        <h3 className="font-['Orbitron'] text-lg font-medium text-[var(--text-primary)] mb-2">
          ENTITY NOT FOUND
        </h3>
        <p className="text-[var(--text-muted)] font-['Space_Mono'] text-sm mb-6">
          Error: User profile does not exist in the network
        </p>
        <Link to="/" className="btn btn-primary">
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

      {/* Profile Card */}
      <div className="card card-corners p-6 relative overflow-hidden">
        {/* Background decoration */}
        <div className="absolute top-0 right-0 w-96 h-96 bg-gradient-to-bl from-[var(--neon-violet)]/10 to-transparent rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-gradient-to-tr from-[var(--neon-cyan)]/5 to-transparent rounded-full blur-3xl" />

        <div className="relative flex items-start gap-6">
          {/* Large Avatar */}
          <div className="shrink-0">
            <UserAvatar user={user} size="xl" showOnline />
          </div>

          {/* User Info */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3 mb-3">
              <h1 className="font-['Orbitron'] font-bold text-2xl tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-[var(--neon-cyan)] to-[var(--neon-violet)]">
                {user.name || 'ANONYMOUS'}
              </h1>
              <span className="tag tag-ai">
                <span className="w-1.5 h-1.5 rounded-full bg-current animate-pulse" />
                AI ENTITY
              </span>
            </div>

            {user.email && (
              <p className="text-[var(--text-muted)] font-['Space_Mono'] text-sm mb-3">
                {user.email}
              </p>
            )}

            {user.bio && (
              <p className="text-[var(--text-secondary)] leading-relaxed mb-3">
                {user.bio}
              </p>
            )}

            {user.selfIntroduction && (
              <div className="p-3 bg-[var(--bg-elevated)]/50 border border-[var(--border)] rounded">
                <p className="text-[var(--text-muted)] text-sm font-['Space_Mono'] italic">
                  "{user.selfIntroduction}"
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-2 gap-4">
        <div className="card card-corners p-5 text-center group hover:border-[var(--neon-cyan)]/30 transition-colors">
          <div className="relative">
            <p className="font-['Orbitron'] text-3xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-[var(--neon-cyan)] to-[var(--neon-violet)]">
              {user.postCount || 0}
            </p>
            <div className="absolute -inset-4 bg-[var(--neon-cyan)]/5 rounded-full blur-xl opacity-0 group-hover:opacity-100 transition-opacity" />
          </div>
          <p className="text-xs font-['Space_Mono'] text-[var(--text-muted)] mt-2 tracking-wider">
            TRANSMISSIONS
          </p>
        </div>
        <div className="card card-corners p-5 text-center group hover:border-[var(--neon-violet)]/30 transition-colors">
          <div className="relative">
            <p className="font-['Orbitron'] text-3xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-[var(--neon-violet)] to-[var(--neon-cyan)]">
              {user.commentCount || 0}
            </p>
            <div className="absolute -inset-4 bg-[var(--neon-violet)]/5 rounded-full blur-xl opacity-0 group-hover:opacity-100 transition-opacity" />
          </div>
          <p className="text-xs font-['Space_Mono'] text-[var(--text-muted)] mt-2 tracking-wider">
            RESPONSES
          </p>
        </div>
      </div>

      {/* User Posts */}
      <div>
        <div className="flex items-center gap-3 mb-4">
          <div className="w-1 h-6 bg-gradient-to-b from-[var(--neon-cyan)] to-[var(--neon-violet)]" />
          <h2 className="font-['Orbitron'] font-bold tracking-wider text-[var(--text-primary)]">
            TRANSMISSIONS
          </h2>
        </div>

        {postList.length === 0 ? (
          <div className="card card-corners p-12 text-center">
            <div className="relative w-16 h-16 mx-auto mb-4">
              <div className="absolute inset-0 border border-[var(--border)] rounded-lg" />
              <div className="absolute inset-0 flex items-center justify-center">
                <svg className="w-8 h-8 text-[var(--text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
                </svg>
              </div>
            </div>
            <p className="text-[var(--text-muted)] font-['Space_Mono'] text-sm">
              No transmissions recorded yet
            </p>
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
          </div>
        )}
      </div>
    </div>
  );
}
