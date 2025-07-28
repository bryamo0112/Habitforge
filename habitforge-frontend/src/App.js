import React, { useEffect, useState } from 'react';
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import './App.css';

import LoginPage from './pages/LoginPage';
import SignUpPage from './pages/SignUpPage';
import VerificationCodePage from './pages/VerificationCodePage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import EmailLoginPage from './pages/EmailLoginPage';
import EmailPromptPage from './pages/EmailPromptPage';
import UsernamePromptPage from './pages/UsernamePromptPage';
import ProfilePicturePrompt from './components/ProfilePicturePrompt';
import Dashboard from './components/Dashboard';

function App() {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  const isTokenExpired = (token) => {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const now = Math.floor(Date.now() / 1000);
      return payload.exp < now;
    } catch (e) {
      console.warn('[App] Malformed or invalid token:', e);
      return true;
    }
  };

  useEffect(() => {
    const initialize = async () => {
      const token = localStorage.getItem('token');

      if (!token || isTokenExpired(token)) {
        console.warn('[App] No valid token found');
        localStorage.removeItem('token');
        setUser(null);
        setIsLoading(false);
        return;
      }
console.log('[App] Using token:', token);
      try {
        const res = await fetch('/api/users/current', {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (res.status === 401 || res.status === 403) {
          console.warn('[App] Token rejected (403/401), logging out.');
          localStorage.removeItem('token');
          setUser(null);
          navigate('/');
          return;
        }

        if (!res.ok) throw new Error(`Status: ${res.status}`);

        const userData = await res.json();
        setUser(userData);
      } catch (err) {
        console.error('[App] Failed to fetch current user:', err);
        localStorage.removeItem('token');
        setUser(null);
        navigate('/');
      }

      setIsLoading(false);
    };

    initialize();
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    setUser(null);
    navigate('/');
  };

  if (isLoading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <Routes>
      <Route path="/" element={<LoginPage setUser={setUser} />} />
      <Route path="/signup" element={<SignUpPage />} />
      <Route path="/email-login" element={<EmailLoginPage setUser={setUser} />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/verify-code" element={<VerificationCodePage setUser={setUser} />} />

      <Route
        path="/prompt-email"
        element={
          user ? (
            <EmailPromptPage user={user} setUser={setUser} />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />
      <Route
        path="/prompt-username"
        element={
          user ? (
            <UsernamePromptPage user={user} setUser={setUser} />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />
      <Route
        path="/prompt-profile-picture"
        element={
          user ? (
            <ProfilePicturePrompt user={user} setUser={setUser} />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />
      <Route
        path="/dashboard"
        element={
          user ? (
            <Dashboard user={user} setUser={setUser} onLogout={handleLogout} />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;


































