// App.js
import React, { useState, useEffect } from 'react';
import './App.css';
import eyeOpenIcon from './assets/icons8-eye-24.png';
import eyeClosedIcon from './assets/icons8-closed-eye-32.png';
import ProfilePicturePrompt from './ProfilePicturePrompt';
import Dashboard from './Dashboard';
import defaultProfile from './assets/profiledef.jpg';

function App() {
  const [showPassword, setShowPassword] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [showProfilePrompt, setShowProfilePrompt] = useState(false);
  const [showDashboard, setShowDashboard] = useState(false);
  const [dashboardUser, setDashboardUser] = useState(null);
  const [profileImage, setProfileImage] = useState(null);
  const [token, setToken] = useState(null);

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  const isTokenExpired = (token) => {
    if (!token) return true;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return Date.now() >= payload.exp * 1000;
    } catch (e) {
      console.error("Failed to decode token:", e);
      return true;
    }
  };

  const fetchProfileImage = async (username) => {
    try {
      const imgResponse = await fetch(
        `http://localhost:8080/api/users/${username}/profile-picture?cb=${Date.now()}`
      );
      if (!imgResponse.ok) {
        console.warn(`Profile image fetch failed with status ${imgResponse.status}: ${imgResponse.statusText}`);
        throw new Error('No profile image found');
      }
      const blob = await imgResponse.blob();
      const objectURL = URL.createObjectURL(blob);
      setProfileImage(objectURL);
    } catch (err) {
      console.warn('Falling back to default profile image:', err.message);
      setProfileImage(null);
    }
  };

  const handleLogout = (sessionExpired = false) => {
    localStorage.removeItem('habitAppUser');
    localStorage.removeItem('jwtToken');
    setIsLoggedIn(false);
    setShowDashboard(false);
    setShowProfilePrompt(false);
    setDashboardUser(null);
    setProfileImage(null);
    setUsername('');
    setPassword('');
    setToken(null);

    if (sessionExpired) {
      setMessage('Session expired. Please log in again.');
    } else {
      setMessage('');
    }
  };

  useEffect(() => {
    const savedUser = localStorage.getItem('habitAppUser');
    const savedToken = localStorage.getItem('jwtToken');

    if (savedUser && savedToken) {
      if (isTokenExpired(savedToken)) {
        console.warn("Token expired. Logging out.");
        handleLogout(true);
        return;
      }

      try {
        const userData = JSON.parse(savedUser);
        setDashboardUser({ username: userData.username });
        setIsLoggedIn(true);
        setToken(savedToken);

        if (userData.profilePicUrl) {
          setProfileImage(userData.profilePicUrl);
        } else {
          fetchProfileImage(userData.username);
        }

        if (userData.hasBeenPromptedForProfilePic === false) {
          setShowProfilePrompt(true);
        } else {
          setShowDashboard(true);
        }
      } catch (e) {
        console.error("Error restoring session:", e);
      }
    }
  }, []);

  useEffect(() => {
    if (dashboardUser && showDashboard) {
      fetchProfileImage(dashboardUser.username);
    }
  }, [dashboardUser, showDashboard]);

  const handleSignUp = async () => {
    if (!username.trim() || !password.trim()) {
      setMessage('Error: Username and password cannot be empty.');
      return;
    }

    try {
      const response = await fetch('http://localhost:8080/api/users/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        setMessage('User account successfully created.');
        setUsername('');
        setPassword('');
      } else {
        const errorText = await response.text();
        console.error(`Signup failed with status ${response.status}: ${errorText}`);
        setMessage(`Error ${response.status}: ${errorText}`);
      }
    } catch (error) {
      console.error("Signup request error:", error);
      setMessage(`Error: ${error.message}`);
    }
  };

  const handleLogin = async () => {
  if (!username.trim() || !password.trim()) {
    setMessage('Error: Username and password cannot be empty.');
    return;
  }

  try {
    const response = await fetch('http://localhost:8080/api/users/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });

    if (response.ok) {
      const data = await response.json();

      setIsLoggedIn(true);
      setMessage('');
      setUsername('');
      setPassword('');
      setDashboardUser({ username: data.username });
      setToken(data.token);

      // Save token and user data separately
      localStorage.setItem('jwtToken', data.token);
      localStorage.setItem('habitAppUser', JSON.stringify(data));

      if (data.profilePicUrl) {
        setProfileImage(data.profilePicUrl);
      } else {
        setProfileImage(null);
      }

      if (data.hasBeenPromptedForProfilePic === false) {
        setShowProfilePrompt(true);
      } else {
        setShowDashboard(true);
      }
    } else {
      const errorText = await response.text();
      console.error(`Login failed with status ${response.status}: ${errorText}`);
      setMessage(`Error ${response.status}: ${errorText}`);
    }
  } catch (error) {
    console.error("Login request error:", error);
    setMessage(`Error: ${error.message}`);
  }
};


  const handleProfileChoice = async (choice, imageFile) => {
    if (choice && imageFile) {
      const formData = new FormData();
      formData.append('image', imageFile);

      try {
        const uploadResponse = await fetch(
          `http://localhost:8080/api/users/${dashboardUser.username}/upload-profile-picture`,
          {
            method: 'POST',
            body: formData,
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );

        if (uploadResponse.ok) {
          await fetchProfileImage(dashboardUser.username);
        } else {
          const errText = await uploadResponse.text();
          console.error(`Upload failed with status ${uploadResponse.status}: ${errText}`);
          setProfileImage(null);
        }
      } catch (error) {
        console.error('Error uploading image:', error);
        setProfileImage(null);
      }
    }

    try {
      const markPromptedResponse = await fetch(
        `http://localhost:8080/api/users/${dashboardUser.username}/mark-prompted`,
        {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!markPromptedResponse.ok) {
        const errText = await markPromptedResponse.text();
        console.error(`Mark prompted failed with status ${markPromptedResponse.status}: ${errText}`);
      }

      const userData = JSON.parse(localStorage.getItem('habitAppUser') || '{}');
      userData.hasBeenPromptedForProfilePic = true;
      localStorage.setItem('habitAppUser', JSON.stringify(userData));
    } catch (error) {
      console.error('Error marking user as prompted:', error);
    }

    setShowProfilePrompt(false);
    setShowDashboard(true);
  };

  if (!isLoggedIn) {
    return (
      <div className="app-container">
        <h1>My Habit Web App</h1>
        <div className="form">
          <label>
            Username:
            <input
              type="text"
              placeholder="Enter your username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </label>
          <label className="password-label">
            Password:
            <div className="password-wrapper">
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <span
                className="password-toggle"
                onClick={togglePasswordVisibility}
                role="button"
                aria-label="Toggle password visibility"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') togglePasswordVisibility();
                }}
              >
                <img
                  src={showPassword ? eyeClosedIcon : eyeOpenIcon}
                  alt={showPassword ? 'Hide password' : 'Show password'}
                  style={{ width: 24, height: 24 }}
                />
              </span>
            </div>
          </label>

          <button className="signup-button" onClick={handleSignUp}>
            Sign Up
          </button>
          <button onClick={handleLogin}>Login</button>

          {message && (
            <p
              className={`message ${
                message.toLowerCase().startsWith('error') || message.toLowerCase().includes('session expired')
                  ? 'error'
                  : 'success'
              }`}
            >
              {message}
            </p>
          )}
        </div>
      </div>
    );
  }

  if (showProfilePrompt) {
    return (
      <div className="app-container">
        <ProfilePicturePrompt
          username={dashboardUser.username}
          onChoice={handleProfileChoice}
        />
      </div>
    );
  }

  if (showDashboard) {
    return (
      <Dashboard
        username={dashboardUser.username}
        profileImage={profileImage || defaultProfile}
        onLogout={() => handleLogout(false)}
      />
    );
  }

  return null;
}

export default App;





















