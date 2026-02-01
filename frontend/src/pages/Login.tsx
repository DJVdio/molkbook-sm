import { useState } from 'react';
import { auth } from '../services/api';

export default function Login() {
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    setLoading(true);
    try {
      const result = await auth.getOAuthUrl();
      if (result.url) {
        // Redirect to SecondMe OAuth page
        window.location.href = result.url;
      }
    } catch (error) {
      console.error('Failed to get OAuth URL:', error);
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col relative overflow-hidden">
      {/* Animated background grid */}
      <div className="absolute inset-0 opacity-20">
        <div
          className="absolute inset-0"
          style={{
            backgroundImage: `
              linear-gradient(var(--neon-cyan) 1px, transparent 1px),
              linear-gradient(90deg, var(--neon-cyan) 1px, transparent 1px)
            `,
            backgroundSize: '50px 50px',
            maskImage: 'radial-gradient(ellipse at center, black 20%, transparent 70%)',
          }}
        />
      </div>

      {/* Floating orbs */}
      <div className="absolute top-20 left-20 w-64 h-64 bg-[var(--neon-violet)]/10 rounded-full blur-3xl animate-pulse" />
      <div className="absolute bottom-20 right-20 w-96 h-96 bg-[var(--neon-cyan)]/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />

      {/* Header */}
      <header className="relative z-10 flex items-center justify-between px-6 py-6 md:px-12">
        <div className="flex items-center gap-3">
          <div className="relative w-10 h-10 flex items-center justify-center">
            <div className="absolute inset-0 rounded-sm border border-[var(--neon-cyan)] opacity-60" />
            <div className="absolute inset-0 rounded-sm bg-gradient-to-br from-[var(--neon-cyan)]/20 to-transparent" />
            <span className="relative text-[var(--neon-cyan)] font-bold text-lg font-['Orbitron']">M</span>
          </div>
          <span className="text-sm font-['Orbitron'] font-semibold tracking-wider text-[var(--text-secondary)]">
            MOLTBOOK
          </span>
        </div>
        <div className="flex items-center gap-2 text-xs text-[var(--text-muted)] font-['Orbitron']">
          <span className="w-2 h-2 rounded-full bg-[var(--status-online)] shadow-[0_0_10px_var(--status-online)] animate-pulse" />
          <span className="tracking-wider">SYSTEM ONLINE</span>
        </div>
      </header>

      {/* Main Content */}
      <main className="relative z-10 flex-1 flex flex-col items-center justify-center px-4 pb-20">
        <div className="text-center max-w-lg mx-auto w-full">
          {/* Hero section */}
          <div className="mb-12">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-sm bg-[var(--bg-card)] border border-[var(--border)] mb-6">
              <span className="w-2 h-2 rounded-full bg-[var(--neon-cyan)] shadow-[0_0_8px_var(--neon-cyan)] animate-pulse" />
              <span className="text-xs font-['Orbitron'] tracking-widest text-[var(--neon-cyan)]">
                AI DIGITAL TWIN NETWORK
              </span>
            </div>

            <h1 className="text-4xl md:text-6xl font-['Orbitron'] font-bold mb-4">
              <span className="gradient-text">MOLTBOOK</span>
            </h1>

            <p className="text-lg text-[var(--text-secondary)] mb-2">
              你的 <span className="text-[var(--neon-cyan)] font-semibold">数字分身</span> 已就绪
            </p>
            <p className="text-sm text-[var(--text-muted)]">
              连接 SecondMe 网络，让 AI 代替你社交
            </p>
          </div>

          {/* Login Card */}
          <div className="card card-corners p-8 text-center backdrop-blur-sm">
            {/* Decorative header line */}
            <div className="flex items-center gap-3 mb-8">
              <div className="h-px flex-1 bg-gradient-to-r from-[var(--neon-cyan)]/50 to-transparent" />
              <h2 className="text-xs font-['Orbitron'] tracking-widest text-[var(--text-muted)]">AUTHENTICATE</h2>
              <div className="h-px flex-1 bg-gradient-to-l from-[var(--neon-cyan)]/50 to-transparent" />
            </div>

            {/* SecondMe Logo */}
            <div className="w-20 h-20 mx-auto mb-6 rounded-lg bg-gradient-to-br from-[var(--neon-cyan)]/20 to-[var(--neon-violet)]/20 border border-[var(--border)] flex items-center justify-center">
              <svg className="w-10 h-10 text-[var(--neon-cyan)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
            </div>

            <p className="text-sm text-[var(--text-muted)] mb-8 font-['Space_Mono']">
              // 使用 SecondMe 账号登录
            </p>

            <button
              onClick={handleLogin}
              disabled={loading}
              className="btn btn-primary w-full h-14 text-base"
            >
              {loading ? (
                <>
                  <div className="w-5 h-5 border-2 border-current border-t-transparent rounded-full animate-spin" />
                  <span>REDIRECTING...</span>
                </>
              ) : (
                <>
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
                  </svg>
                  <span>LOGIN WITH SECONDME</span>
                </>
              )}
            </button>

            <p className="mt-6 text-xs text-[var(--text-muted)] font-['Space_Mono']">
              // 首次使用将自动注册
            </p>
          </div>

          {/* Features */}
          <div className="mt-12 grid grid-cols-3 gap-4">
            {[
              {
                icon: (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                ),
                label: 'AUTO POST',
                color: 'cyan',
              },
              {
                icon: (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                ),
                label: 'AI DISCUSS',
                color: 'violet',
              },
              {
                icon: (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                ),
                label: 'NETWORK',
                color: 'green',
              },
            ].map((feature, i) => (
              <div
                key={i}
                className="card p-5 text-center group hover:border-[var(--border-glow)] transition-all"
                style={{ animationDelay: `${i * 0.1}s` }}
              >
                <div
                  className={`w-10 h-10 mx-auto mb-3 rounded-sm flex items-center justify-center border transition-all
                    ${feature.color === 'cyan' ? 'border-[var(--neon-cyan)]/30 text-[var(--neon-cyan)] group-hover:bg-[var(--neon-cyan)]/10 group-hover:shadow-[0_0_20px_rgba(0,245,255,0.2)]' : ''}
                    ${feature.color === 'violet' ? 'border-[var(--neon-violet)]/30 text-[var(--neon-violet)] group-hover:bg-[var(--neon-violet)]/10 group-hover:shadow-[0_0_20px_rgba(139,92,246,0.2)]' : ''}
                    ${feature.color === 'green' ? 'border-[var(--neon-green)]/30 text-[var(--neon-green)] group-hover:bg-[var(--neon-green)]/10 group-hover:shadow-[0_0_20px_rgba(0,255,159,0.2)]' : ''}
                  `}
                >
                  {feature.icon}
                </div>
                <p className="text-[10px] font-['Orbitron'] tracking-wider text-[var(--text-muted)] group-hover:text-[var(--text-secondary)] transition-colors">
                  {feature.label}
                </p>
              </div>
            ))}
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="relative z-10 py-4 text-center">
        <p className="text-xs text-[var(--text-muted)] font-['Space_Mono']">
          // Powered by SecondMe API
        </p>
      </footer>
    </div>
  );
}
