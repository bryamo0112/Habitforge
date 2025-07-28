import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './ResetPasswordPage.css';

const ResetPasswordPage = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [message, setMessage] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);

  // Optional: Prepopulate from verification page
  useEffect(() => {
    if (location.state?.email) setEmail(location.state.email);
    if (location.state?.code) setCode(location.state.code);
  }, [location.state]);

  const handleReset = async (e) => {
    e.preventDefault();
    setMessage('');
    setErrorMsg('');
    setLoading(true);

    try {
      const res = await fetch('/api/users/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, code, newPassword }),
      });

      if (res.ok) {
        setMessage('Password reset successful! Redirecting to login...');
        setTimeout(() => navigate('/'), 2000);
      } else {
        // Try parsing JSON error message, fallback to plain text
        let errorText = await res.text();
        try {
          const data = JSON.parse(errorText);
          setErrorMsg(data.message || errorText);
        } catch {
          setErrorMsg(errorText);
        }
      }
    } catch (err) {
      console.error('Reset password error:', err);
      setErrorMsg('Server error while resetting password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="reset-password-container">
      <h2>Reset Password</h2>
      <p>Enter your email, the reset code, and your new password.</p>
      <form onSubmit={handleReset}>
        <input
          type="email"
          placeholder="Email address"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="text"
          placeholder="Reset code"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="New password (min 6 characters)"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          required
          minLength={6}
        />

        {message && <div className="info-message">{message}</div>}
        {errorMsg && <div className="error-message">{errorMsg}</div>}

        <button type="submit" disabled={loading}>
          {loading ? 'Resetting...' : 'Reset Password'}
        </button>
      </form>
    </div>
  );
};

export default ResetPasswordPage;



