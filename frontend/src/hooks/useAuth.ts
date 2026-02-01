import { useState, useEffect } from 'react';
import type { User } from '../types';
import { auth } from '../services/api';

export function useAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      setLoading(false);
      return;
    }

    try {
      const result = await auth.verify();
      if (result.valid && result.user) {
        setUser(result.user);
        setIsAuthenticated(true);
      } else {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    } catch {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    } finally {
      setLoading(false);
    }
  };


  const logout = () => {
    auth.logout();
    setUser(null);
    setIsAuthenticated(false);
  };

  const setUserAndToken = (token: string, userData: User) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    setIsAuthenticated(true);
  };

  return {
    user,
    loading,
    isAuthenticated,
    logout,
    checkAuth,
    setUserAndToken,
  };
}
