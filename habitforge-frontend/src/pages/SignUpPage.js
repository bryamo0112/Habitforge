import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './SignUpPage.css';
import openEye from '../components/assets/icons8-eye-24.png';
import closedEye from '../components/assets/icons8-closed-eye-32.png';


const SignUpPage = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [showPassword, setShowPassword] = useState(false);

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

        <div className="password-input-container2">
  <input
    type={showPassword ? 'text' : 'password'}
    placeholder="Password"
    value={password}
    onChange={(e) => setPassword(e.target.value)}
    required
    name="password"
    aria-label="Password"
    autoComplete="current-password"
  />
  <img
    src={showPassword ? openEye : closedEye}
    alt={showPassword ? 'Hide password' : 'Show password'}
    onClick={() => setShowPassword(prev => !prev)}
    className="password-toggle-icon2"
  />
</div>


        {errorMsg && <div className="error-message">{errorMsg}</div>}
        {successMsg && <div className="success-message">{successMsg}</div>}

        <button type="submit">Sign Up</button>
      </form>
    </div>
  );
};

export default SignUpPage;

