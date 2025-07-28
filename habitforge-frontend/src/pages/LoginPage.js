import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginPage.css';

const LoginPage = ({ setUser }) => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // Step 1: Login to get JWT token or trigger email verification
      const loginRes = await fetch('/api/users/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (loginRes.status === 202) {
        // Email verification required (only if not verified)
        const data = await loginRes.json();
        console.log('[Login] Verification code required:', data);
        localStorage.setItem('pendingEmailVerification', data.partialToken);
        navigate('/verify-code');
        return;
      }

      if (!loginRes.ok) {
        const errorData = await loginRes.json().catch(() => ({}));
        throw new Error(errorData.error || errorData.message || 'Login failed');
      }

      const loginData = await loginRes.json();
      const token = loginData?.token;

      if (!token || typeof token !== 'string' || token.split('.').length !== 3) {
        console.error('[Login] Received invalid token:', token);
        throw new Error('Invalid token received from server.');
      }

      // Save token to localStorage
      localStorage.setItem('token', token);

      // Step 2: Use token to fetch full user info
      const userRes = await fetch('/api/users/current', {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!userRes.ok) {
        const errText = await userRes.text();
        console.error('[Login] Failed to fetch user:', errText);
        throw new Error('Failed to retrieve user data after login');
      }

      const userData = await userRes.json();
      setUser(userData);

      // Step 3: Route user based on missing fields
      if (!userData.email) {
  navigate('/prompt-email');
} else if (!userData.emailVerified) {
  navigate('/verify-code');
} else if (!userData.username) {
  navigate('/prompt-username');
} else if (!userData.profilePicUrl) {
  navigate('/prompt-profile-picture');
} else {
  navigate('/dashboard');
}

    } catch (err) {
      console.error('[Login] Error during login flow:', err);
      setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <form className="login-form" onSubmit={handleLogin} aria-label="Login form">
        <h2>Login</h2>
        {error && <p className="error" role="alert">{error}</p>}
        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          name="username"
          aria-label="Username"
          autoComplete="username"
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          name="password"
          aria-label="Password"
          autoComplete="current-password"
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
        <p className="link" onClick={() => navigate('/email-login')} role="button" tabIndex={0}>
          Login with email instead
        </p>
        <p className="link" onClick={() => navigate('/signup')} role="button" tabIndex={0}>
          Don’t have an account? Sign up
        </p>
        <p className="link" onClick={() => navigate('/forgot-password')} role="button" tabIndex={0}>
          Forgot password?
        </p>
      </form>
    </div>
  );
};

export default LoginPage;





