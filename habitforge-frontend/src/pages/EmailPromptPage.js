import React, { useState, useEffect } from 'react'; 
import { useNavigate } from 'react-router-dom';
import './EmailPromptPage.css';

const EmailPromptPage = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [infoMsg, setInfoMsg] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Clear messages on email change for better UX
    setErrorMsg('');
    setInfoMsg('');
  }, [email]);

  const handleEmailSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setInfoMsg('');
    setLoading(true);

    const token = localStorage.getItem('token');
    if (!token) {
      setErrorMsg('Session expired. Please log in again.');
      setLoading(false);
      return;
    }

    try {
      const response = await fetch('/api/users/set-email', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ email }),
      });

      if (response.ok) {
        setInfoMsg('Verification code sent to your email!');
        // Navigate to verification code page and pass email via state
        navigate('/verify-code', { state: { email } });
      } else {
        const data = await response.json();
        setErrorMsg(data.message || 'Email could not be saved');
      }
    } catch (err) {
      console.error('Email submission error:', err);
      setErrorMsg('Server error while submitting email');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="email-prompt-container">
      <h2>Add an Email</h2>
      <p>This helps with password recovery and receiving habit reminders.</p>
      <form onSubmit={handleEmailSubmit}>
        <input
          type="email"
          placeholder="Enter a valid email address"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          autoFocus
        />

        {errorMsg && <div className="error-message">{errorMsg}</div>}
        {infoMsg && <div className="info-message">{infoMsg}</div>}

        <button type="submit" disabled={loading}>
          {loading ? 'Sending Code...' : 'Submit Email'}
        </button>
      </form>
    </div>
  );
};

export default EmailPromptPage;

