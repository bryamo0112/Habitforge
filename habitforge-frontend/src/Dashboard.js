import React, { useEffect, useState } from 'react';
import defaultProfile from './assets/profiledef.jpg';
import logo from './assets/HabitForgeLogo.png'; // <-- Import logo
import './Dashboard.css';

function Dashboard({ username, profileImage, onLogout }) {
  const [habits, setHabits] = useState([]);
  const [newHabitTitle, setNewHabitTitle] = useState('');
  const [newHabitTargetDays, setNewHabitTargetDays] = useState('');
  const token = localStorage.getItem('jwtToken');

  useEffect(() => {
    const fetchHabits = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/habits', {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (res.ok) {
          const data = await res.json();
          setHabits(data);
        } else {
          console.error('Failed to fetch habits');
        }
      } catch (err) {
        console.error('Error fetching habits:', err);
      }
    };

    fetchHabits();
  }, [token]);

  const handleCheckIn = async (habitId) => {
    try {
      const res = await fetch(`http://localhost:8080/api/habits/${habitId}/check-in`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        const updatedHabits = await fetch('http://localhost:8080/api/habits', {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (updatedHabits.ok) {
          const data = await updatedHabits.json();
          setHabits(data);
        }
      } else {
        alert(await res.text());
      }
    } catch (err) {
      console.error('Error checking in:', err);
    }
  };

  const handleCreateHabit = async () => {
    const title = newHabitTitle.trim();
    const days = parseInt(newHabitTargetDays, 10);

    if (!title || isNaN(days) || days <= 0) {
      alert('Please enter a valid title and target days.');
      return;
    }

    try {
      const res = await fetch('http://localhost:8080/api/habits/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ title, targetDays: days }),
      });

      if (res.ok) {
        setNewHabitTitle('');
        setNewHabitTargetDays('');
        const updatedHabits = await fetch('http://localhost:8080/api/habits', {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (updatedHabits.ok) {
          const data = await updatedHabits.json();
          setHabits(data);
        }
      } else {
        alert('Failed to create habit.');
      }
    } catch (err) {
      console.error('Error creating habit:', err);
    }
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        {/* Left: Logo + welcome */}
        <div className="logo-container">
          <span className="welcome-text">Welcome to</span>
          <img src={logo} alt="HabitForge Logo" className="dashboard-logo" />
        </div>

        {/* Right: Greeting + logout */}
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

      <main className="dashboard-main">
        <div className="habit-form">
          <input
            type="text"
            placeholder="Habit title"
            value={newHabitTitle}
            onChange={(e) => setNewHabitTitle(e.target.value)}
          />
          <input
            type="number"
            placeholder="Target days"
            value={newHabitTargetDays}
            onChange={(e) => setNewHabitTargetDays(e.target.value)}
          />
          <button className="create-habit-button" onClick={handleCreateHabit}>
            Create Habit
          </button>
        </div>

        <div className="habit-list">
          {habits.length === 0 ? (
            <p>No habits yet. Add one above!</p>
          ) : (
            habits.map((habit) => (
              <div key={habit.id} className="habit-card">
                <h3>{habit.title}</h3>
                <p>
                  Streak: {habit.currentStreak} / {habit.targetDays} days
                </p>
                <div className="progress-bar">
                  <div
                    className="progress-fill"
                    style={{
                      width: `${Math.min(
                        (habit.currentStreak / habit.targetDays) * 100,
                        100
                      )}%`,
                    }}
                  ></div>
                </div>
                <button className="checkin-button" onClick={() => handleCheckIn(habit.id)}>
                  Check In
                </button>
              </div>
            ))
          )}
        </div>
      </main>
    </div>
  );
}

export default Dashboard;










