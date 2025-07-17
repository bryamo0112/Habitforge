// Dashboard.js
import React from 'react';
import defaultProfile from './assets/profiledef.jpg';
import './Dashboard.css';

function Dashboard({ username, profileImage, onLogout }) {
  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        {/* Left side: Habit buttons */}
        <div className="dashboard-left-buttons">
          <button className="dashboard-btn">Write down habit</button>
          <button className="dashboard-btn">Write down goal</button>
          <button className="dashboard-btn">Write down time frame</button>
        </div>

        {/* Right side: Greeting and Logout */}
        <div className="greeting-container">
          <div className="greeting">
            Hello, {username}
            <img
              src={profileImage || defaultProfile}
              alt="Profile"
              className="dashboard-profile-pic"
            />
          </div>
          <button className="logout-button" onClick={onLogout}>
            Logout
          </button>
        </div>
      </header>

      {/* TODO: Add dashboard content here */}
    </div>
  );
}

export default Dashboard;



