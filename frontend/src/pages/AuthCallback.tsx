import { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { auth } from '../services/api';
import type { User } from '../types';

interface AuthCallbackProps {
  onLoginSuccess: (token: string, user: User) => void;
}

export default function AuthCallback({ onLoginSuccess }: AuthCallbackProps) {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const processedRef = useRef(false);

  const handleCallback = useCallback(async (code: string) => {
    try {
      const result = await auth.handleCallback(code);
      if (result.success && result.token && result.user) {
        onLoginSuccess(result.token, result.user);
        navigate('/');
      } else {
        setError(result.error || 'Authentication failed');
      }
    } catch (err) {
      setError('Failed to complete authentication');
    }
  }, [onLoginSuccess, navigate]);

  useEffect(() => {
    // Prevent double execution in React Strict Mode
    if (processedRef.current) {
      return;
    }

    const code = searchParams.get('code');
    const errorParam = searchParams.get('error');

    if (errorParam) {
      setError(errorParam);
      return;
    }

    if (code) {
      processedRef.current = true;
      handleCallback(code);
    } else {
      setError('No authorization code provided');
    }
  }, [searchParams, handleCallback]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="card card-corners p-8 text-center max-w-md">
          <div className="w-16 h-16 mx-auto mb-6 rounded-full bg-red-500/10 border border-red-500/30 flex items-center justify-center">
            <svg className="w-8 h-8 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h2 className="font-['Orbitron'] text-lg font-bold text-[var(--text-primary)] mb-2">
            AUTHENTICATION FAILED
          </h2>
          <p className="text-[var(--text-muted)] font-['Space_Mono'] text-sm mb-6">
            {error}
          </p>
          <button
            onClick={() => navigate('/login')}
            className="btn btn-primary"
          >
            RETURN TO LOGIN
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="card card-corners p-8 text-center">
        <div className="relative inline-block mb-6">
          <div className="w-16 h-16 border-2 border-[var(--neon-cyan)] border-t-transparent rounded-full animate-spin mx-auto" />
          <div className="absolute inset-0 w-16 h-16 border-2 border-[var(--neon-violet)] border-b-transparent rounded-full animate-spin mx-auto" style={{ animationDirection: 'reverse', animationDuration: '1.5s' }} />
        </div>
        <h2 className="font-['Orbitron'] text-lg font-bold text-[var(--text-primary)] mb-2">
          AUTHENTICATING
        </h2>
        <p className="text-[var(--text-muted)] font-['Space_Mono'] text-sm">
          Connecting to SecondMe network...
        </p>
      </div>
    </div>
  );
}
