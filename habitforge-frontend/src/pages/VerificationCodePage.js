import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import './VerificationCodePage.css';

const VerificationCodePage = ({ setUser }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const [code, setCode] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendDisabled, setResendDisabled] = useState(false);

  const emailFromState = location.state?.email;
  const [email, setEmail] = useState(emailFromState || '');

  // Purpose can be 'login' (default) or 'reset' for password reset flow
  const purpose = location.state?.purpose || 'login';

  useEffect(() => {
    if (!emailFromState) {
      const savedEmail = localStorage.getItem('pendingEmailVerification');
      if (savedEmail) {
        setEmail(savedEmail);
      } else {
        setErrorMsg('No email provided. Please start the login process again.');
        setTimeout(() => {
          navigate('/login');
        }, 3000);
      }
    } else {
      localStorage.setItem('pendingEmailVerification', emailFromState);
    }
  }, [emailFromState, navigate]);

  const handleVerify = async (e) => {
    e.preventDefault();

    if (!email) {
      setErrorMsg('No email provided. Please start the login process again.');
      return;
    }

    setLoading(true);
    setErrorMsg('');
    setSuccessMsg('');

    try {
      const response = await fetch('/api/users/verify-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, code }),
      });

      if (response.ok) {
        const data = await response.json();

        if (purpose === 'reset') {
          // For password reset flow: skip login token, redirect to reset-password page with email
          localStorage.removeItem('pendingEmailVerification');
          navigate('/reset-password', { state: { email } });
          return;
        }

        const token = data.token;

        if (!token) {
          throw new Error('No token received from server.');
        }

        localStorage.setItem('token', token);
        localStorage.removeItem('pendingEmailVerification');

        const userResponse = await fetch('/api/users/current', {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!userResponse.ok) {
          throw new Error('Failed to fetch user info after verification');
        }

        const userData = await userResponse.json();
        setUser(userData);

        if (!userData.username || userData.username.startsWith('user_')) {
          navigate('/prompt-username');
        } else if (!userData.profilePicUrl) {
          navigate('/prompt-profile-picture');
        } else {
          navigate('/dashboard');
        }
      } else {
        const result = await response.json();
        setErrorMsg(result.error || result.message || 'Invalid verification code');
      }
    } catch (error) {
      console.error('Verification error:', error);
      setErrorMsg('Server error during verification');
    } finally {
      setLoading(false);
    }
  };

  const handleResendCode = async () => {
    if (!email) {
      setErrorMsg('No email provided. Please restart the login process.');
      return;
    }

    if (resendDisabled) return;

    setResendDisabled(true);
    setErrorMsg('');
    setSuccessMsg('');

    try {
      const response = await fetch('/api/users/send-verification-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });

      if (response.ok) {
        setSuccessMsg('A new verification code was sent to your email.');
        setTimeout(() => setResendDisabled(false), 30000);
      } else {
        const data = await response.json();
        setErrorMsg(data.error || data.message || 'Failed to resend code');
        setResendDisabled(false);
      }
    } catch (error) {
      console.error('Resend code error:', error);
      setErrorMsg('Server error while resending code');
      setResendDisabled(false);
    }
  };

  return (
    <div className="verification-container">
      <h2>Enter Verification Code</h2>
      <p>Please check your email <strong>{email || ''}</strong> for the 6-digit code.</p>
      <form onSubmit={handleVerify}>
        <input
          type="text"
          placeholder="Enter the 6-digit code"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          required
          maxLength={6}
          pattern="\d{6}"
          aria-label="Verification code"
        />

        {errorMsg && <div className="error-message" role="alert">{errorMsg}</div>}
        {successMsg && <div className="success-message">{successMsg}</div>}

        <button type="submit" disabled={loading}>
          {loading ? 'Verifying...' : 'Verify Code'}
        </button>
      </form>

      <p
        className="link"
        onClick={resendDisabled ? undefined : handleResendCode}
        style={{
          marginTop: '10px',
          cursor: resendDisabled ? 'not-allowed' : 'pointer',
          opacity: resendDisabled ? 0.5 : 1,
          userSelect: 'none',
        }}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if ((e.key === 'Enter' || e.key === ' ') && !resendDisabled) handleResendCode();
        }}
      >
        Didn’t get a code? Resend
      </p>
    </div>
  );
};

export default VerificationCodePage;









