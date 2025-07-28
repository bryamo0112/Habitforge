import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './EmailLoginPage.css';

const EmailLoginPage = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const handleSendCode = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    try {
      const response = await fetch('/api/users/send-verification-code', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email }),
      });

      if (response.ok) {
        setSuccessMsg('Verification code sent!');
        setTimeout(() => {
          navigate('/verify-code', { state: { email } });
        }, 500);
      } else {
        const data = await response.json();
        setErrorMsg(data.message || 'Failed to send verification code');
      }
    } catch (error) {
      console.error('Email login error:', error);
      setErrorMsg('Server error during email login');
    }
  };

  return (
    <div className="email-login-container">
      <h2>Email Login</h2>
      <form onSubmit={handleSendCode}>
        <input
          type="email"
          placeholder="Enter your email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        {errorMsg && <div className="error-message">{errorMsg}</div>}
        {successMsg && <div className="success-message">{successMsg}</div>}

        <button type="submit">Send Verification Code</button>
      </form>
    </div>
  );
};

export default EmailLoginPage;

