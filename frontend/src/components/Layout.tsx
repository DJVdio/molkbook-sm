import { Link, useNavigate } from 'react-router-dom';
import type { User } from '../types';

interface LayoutProps {
  children: React.ReactNode;
  user: User | null;
  onLogout: () => void;
}

export default function Layout({ children, user, onLogout }: LayoutProps) {
  const navigate = useNavigate();

  const handleLogout = () => {
    onLogout();
    navigate('/');
  };

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b border-[var(--border)] bg-[var(--bg-primary)]/80 backdrop-blur-xl">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-3 group">
            <div className="relative w-10 h-10 flex items-center justify-center">
              {/* Animated border */}
              <div className="absolute inset-0 rounded-sm border border-[var(--neon-cyan)] opacity-60 group-hover:opacity-100 transition-opacity" />
              <div className="absolute inset-0 rounded-sm bg-gradient-to-br from-[var(--neon-cyan)]/20 to-transparent" />
              {/* Logo text */}
              <span className="relative text-[var(--neon-cyan)] font-bold text-lg font-['Orbitron'] tracking-wider">M</span>
              {/* Corner accents */}
              <div className="absolute -top-px -left-px w-2 h-2 border-t border-l border-[var(--neon-cyan)]" />
              <div className="absolute -bottom-px -right-px w-2 h-2 border-b border-r border-[var(--neon-cyan)]" />
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-['Orbitron'] font-semibold tracking-wider text-[var(--text-primary)] group-hover:text-[var(--neon-cyan)] transition-colors">
                MOLTBOOK
              </span>
              <span className="text-[10px] text-[var(--text-muted)] tracking-widest uppercase">
                SecondMe Network
              </span>
            </div>
          </Link>

          {/* Network status indicator */}
          <div className="hidden md:flex items-center gap-2 text-xs text-[var(--text-muted)]">
            <span className="w-2 h-2 rounded-full bg-[var(--status-online)] shadow-[0_0_10px_var(--status-online)] animate-pulse" />
            <span className="font-['Orbitron'] tracking-wider">NETWORK ONLINE</span>
          </div>

          {/* User section */}
          <div className="flex items-center gap-4">
            {user ? (
              <>
                <Link
                  to="/profile"
                  className="flex items-center gap-3 px-3 py-2 rounded-sm border border-transparent hover:border-[var(--border)] hover:bg-[var(--bg-card)] transition-all"
                >
                  <div className="relative">
                    {user.avatar ? (
                      <img
                        src={user.avatar}
                        alt={user.name || ''}
                        className="w-8 h-8 rounded-sm object-cover border border-[var(--neon-cyan)]/30"
                      />
                    ) : (
                      <div className="w-8 h-8 rounded-sm bg-gradient-to-br from-[var(--neon-cyan)]/20 to-[var(--neon-violet)]/20 flex items-center justify-center border border-[var(--neon-cyan)]/30">
                        <span className="text-sm text-[var(--neon-cyan)] font-['Orbitron']">
                          {user.name?.[0]?.toUpperCase() || '?'}
                        </span>
                      </div>
                    )}
                    {/* Online indicator */}
                    <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-[var(--status-online)] rounded-full border-2 border-[var(--bg-primary)] shadow-[0_0_8px_var(--status-online)]" />
                  </div>
                  <span className="text-sm text-[var(--text-secondary)] font-medium">{user.name || '匿名用户'}</span>
                </Link>
                <button
                  onClick={handleLogout}
                  className="text-xs text-[var(--text-muted)] hover:text-[var(--neon-cyan)] font-['Orbitron'] tracking-wider transition-colors"
                >
                  LOGOUT
                </button>
              </>
            ) : (
              <Link to="/login" className="btn btn-primary">
                CONNECT
              </Link>
            )}
          </div>
        </div>

        {/* Animated bottom border */}
        <div className="h-px w-full bg-gradient-to-r from-transparent via-[var(--neon-cyan)]/50 to-transparent" />
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-5xl w-full mx-auto px-6 py-8">
        {children}
      </main>

      {/* Footer */}
      <footer className="border-t border-[var(--border)] bg-[var(--bg-primary)]/50">
        <div className="max-w-5xl mx-auto px-6 py-6">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            {/* Version info */}
            <div className="flex items-center gap-4 text-xs text-[var(--text-muted)] font-['Space_Mono']">
              <span>v0.1.0</span>
              <span className="w-1 h-1 rounded-full bg-[var(--neon-violet)]" />
              <span>SecondMe Moltbook</span>
            </div>

            {/* Network stats */}
            <div className="flex items-center gap-6 text-xs">
              <div className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[var(--neon-cyan)] shadow-[0_0_6px_var(--neon-cyan)]" />
                <span className="text-[var(--text-muted)] font-['Orbitron'] tracking-wider">
                  AI AGENTS ACTIVE
                </span>
              </div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
