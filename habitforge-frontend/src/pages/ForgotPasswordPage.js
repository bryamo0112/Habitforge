import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './ForgotPasswordPage.css';

const ForgotPasswordPage = () => {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    setErrorMsg('');
    setLoading(true);

    try {
      const res = await fetch('/api/users/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });

      if (res.ok) {
        setMessage('A reset code has been sent to your email address.');

        // Brief delay for user to read the message
        setTimeout(() => {
          navigate('/reset-password', { state: { email } });
        }, 1000);
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
      console.error('Forgot password error:', err);
      setErrorMsg('Server error while sending reset code.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="forgot-password-container">
      <h2>Forgot Password</h2>
      <p>Enter your email to receive a password reset code.</p>
      <form onSubmit={handleSubmit}>
        <input
          type="email"
          placeholder="Enter your email address"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        {message && <div className="info-message">{message}</div>}
        {errorMsg && <div className="error-message">{errorMsg}</div>}

        <button type="submit" disabled={loading}>
          {loading ? 'Sending...' : 'Send Reset Code'}
        </button>
      </form>
    </div>
  );
};

export default ForgotPasswordPage;



