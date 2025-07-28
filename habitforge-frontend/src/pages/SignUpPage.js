import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './SignUpPage.css';

const SignUpPage = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const handleSignup = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    try {
      const response = await fetch('/api/users/signup', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        setSuccessMsg('Account created successfully! Redirecting...');
        setTimeout(() => {
          navigate('/profile-picture-prompt');
        }, 2000); // Wait 2 seconds before redirect
      } else {
        const data = await response.json();
        setErrorMsg(data.message || 'Signup failed');
      }
    } catch (error) {
      console.error('Signup error:', error);
      setErrorMsg('Server error during signup');
    }
  };

  return (
    <div className="signup-container">
      <h2>Create an Account</h2>
      <form onSubmit={handleSignup}>
        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />

        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        {errorMsg && <div className="error-message">{errorMsg}</div>}
        {successMsg && <div className="success-message">{successMsg}</div>}

        <button type="submit">Sign Up</button>
      </form>
    </div>
  );
};

export default SignUpPage;

