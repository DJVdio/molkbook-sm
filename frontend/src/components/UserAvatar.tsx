import type { User } from '../types';

interface UserAvatarProps {
  user: User;
  size?: 'sm' | 'md' | 'lg';
  showOnline?: boolean;
}

const sizeConfig = {
  sm: { container: 'w-8 h-8', text: 'text-xs', online: 'w-2 h-2' },
  md: { container: 'w-11 h-11', text: 'text-sm', online: 'w-2.5 h-2.5' },
  lg: { container: 'w-16 h-16', text: 'text-xl', online: 'w-3 h-3' },
};

export default function UserAvatar({ user, size = 'md', showOnline = false }: UserAvatarProps) {
  const config = sizeConfig[size];

  return (
    <div className="relative">
      {/* Avatar container */}
      <div className={`${config.container} relative rounded-sm overflow-hidden`}>
        {/* Glow border effect */}
        <div className="absolute inset-0 rounded-sm border border-[var(--neon-cyan)]/30" />

        {user.avatar ? (
          <img
            src={user.avatar}
            alt={user.name || '用户头像'}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full bg-gradient-to-br from-[var(--neon-cyan)]/20 to-[var(--neon-violet)]/20 flex items-center justify-center">
            <span className={`${config.text} text-[var(--neon-cyan)] font-['Orbitron'] font-bold`}>
              {user.name?.[0]?.toUpperCase() || '?'}
            </span>
          </div>
        )}

        {/* Corner decorations */}
        <div className="absolute top-0 left-0 w-1.5 h-1.5 border-t border-l border-[var(--neon-cyan)]/50" />
        <div className="absolute bottom-0 right-0 w-1.5 h-1.5 border-b border-r border-[var(--neon-cyan)]/50" />
      </div>

      {/* Online indicator */}
      {showOnline && (
        <span
          className={`absolute -bottom-0.5 -right-0.5 ${config.online} bg-[var(--status-online)] rounded-full border-2 border-[var(--bg-card)]`}
          style={{
            boxShadow: '0 0 8px var(--status-online)',
          }}
        />
      )}
    </div>
  );
}
