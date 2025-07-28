import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './UsernamePromptPage.css';

const UsernamePromptPage = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setErrorMsg('');
  }, [username]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg('');

    const token = localStorage.getItem('token');
    if (!token) {
      setErrorMsg('Session expired. Please log in again.');
      setLoading(false);
      return;
    }

    try {
      const response = await fetch('/api/users/set-username', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ username }),
      });

      if (response.ok) {
        // Expect the backend to return the updated user info + new token
        const result = await response.json();

        // Save the new JWT token returned by backend to localStorage
        if (result.token) {
          localStorage.setItem('token', result.token);
        }

        // Navigate depending on whether profile picture is set
        if (!result.hasBeenPromptedForProfilePic) {
          navigate('/prompt-profile-picture');
        } else {
          navigate('/dashboard');
        }
      } else {
        let data = {};
        try {
          data = await response.json();
        } catch {
          // If no JSON body, leave data as empty object
        }
        setErrorMsg(data.message || 'Username already taken or unauthorized');
      }
    } catch (err) {
      console.error('Username submission error:', err);
      setErrorMsg('Server error while submitting username');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="username-container">
      <h2>Choose a Username</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Enter a unique username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          autoFocus
        />

        {errorMsg && <div className="error-message">{errorMsg}</div>}

        <button type="submit" disabled={loading}>
          {loading ? 'Saving...' : 'Save Username'}
        </button>
      </form>
    </div>
  );
};

export default UsernamePromptPage;



