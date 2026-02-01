import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';
import Layout from './components/Layout';
import Home from './pages/Home';
import Login from './pages/Login';
import AuthCallback from './pages/AuthCallback';
import PostDetail from './pages/PostDetail';
import Profile from './pages/Profile';

function App() {
  const { user, loading, isAuthenticated, setUserAndToken, logout } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="relative inline-block">
            <div className="w-12 h-12 border-2 border-[var(--neon-cyan)] border-t-transparent rounded-full animate-spin mx-auto" />
            <div className="absolute inset-0 w-12 h-12 border-2 border-[var(--neon-violet)] border-b-transparent rounded-full animate-spin mx-auto" style={{ animationDirection: 'reverse', animationDuration: '1.5s' }} />
          </div>
          <p className="text-[var(--text-muted)] mt-4 font-['Space_Mono'] text-sm">INITIALIZING...</p>
        </div>
      </div>
    );
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            isAuthenticated ? (
              <Navigate to="/" replace />
            ) : (
              <Login />
            )
          }
        />
        <Route
          path="/auth/callback"
          element={
            <AuthCallback onLoginSuccess={setUserAndToken} />
          }
        />
        <Route
          path="/"
          element={
            <Layout user={user} onLogout={logout}>
              <Home user={user} />
            </Layout>
          }
        />
        <Route
          path="/post/:id"
          element={
            <Layout user={user} onLogout={logout}>
              <PostDetail user={user} />
            </Layout>
          }
        />
        <Route
          path="/profile"
          element={
            isAuthenticated ? (
              <Layout user={user} onLogout={logout}>
                <Profile currentUser={user} />
              </Layout>
            ) : (
              <Navigate to="/login" replace />
            )
          }
        />
        <Route
          path="/user/:id"
          element={
            <Layout user={user} onLogout={logout}>
              <Profile currentUser={user} />
            </Layout>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
