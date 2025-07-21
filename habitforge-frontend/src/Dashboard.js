import React, { useEffect, useState, useCallback } from 'react';
import defaultProfile from './assets/profiledef.jpg';
import logo from './assets/HabitForgeLogo.png';
import './Dashboard.css';

// Simple confetti animation function (placeholder)
function triggerConfetti() {
  alert('🎉 Congrats on completing your habit! 🎉');
}

function Dashboard({ username, profileImage, onLogout }) {
  const [habits, setHabits] = useState([]);
  const [newHabitTitle, setNewHabitTitle] = useState('');
  const [newHabitTargetDays, setNewHabitTargetDays] = useState('');
  const [sortBy, setSortBy] = useState('startdate');
  const [filterByStatus, setFilterByStatus] = useState('all'); // all, active, completed
  const [editHabit, setEditHabit] = useState(null);
  const [reminderTimes, setReminderTimes] = useState({});
  const [confirmDeleteHabitId, setConfirmDeleteHabitId] = useState(null);
  const token = localStorage.getItem('jwtToken');

  // Fetch habits with sorting parameter
  const fetchHabits = useCallback(async () => {
    try {
      const url = new URL('http://localhost:8080/api/habits/sorted');
      url.searchParams.append('sortBy', sortBy);

      const res = await fetch(url.toString(), {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        const data = await res.json();
        setHabits(data);

        // Initialize reminder times
        const times = {};
        data.forEach(habit => {
          if (habit.reminderTime) times[habit.id] = habit.reminderTime;
        });
        setReminderTimes(times);

        // Simple welcome back alert every 24h
        const lastLogin = localStorage.getItem('lastLoginTime');
        const now = Date.now();
        if (!lastLogin || now - lastLogin > 24 * 3600 * 1000) {
          alert('Welcome back! Remember to check in on your habits daily.');
          localStorage.setItem('lastLoginTime', now);
        }
      } else {
        console.error('Failed to fetch habits');
      }
    } catch (err) {
      console.error('Error fetching habits:', err);
    }
  }, [token, sortBy]);

  useEffect(() => {
    fetchHabits();
  }, [fetchHabits]);

  // Filter habits client-side
  const filteredHabits = habits.filter(habit => {
    if (filterByStatus === 'active') return !habit.completed;
    if (filterByStatus === 'completed') return habit.completed;
    return true;
  });

  const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD

  // Handle check-in API call
  const handleCheckIn = async habitId => {
    try {
      const res = await fetch(`http://localhost:8080/api/habits/${habitId}/check-in`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        await fetchHabits();

        const habit = habits.find(h => h.id === habitId);
        if (habit && habit.currentStreak + 1 >= habit.targetDays) {
          triggerConfetti();
        }
      } else {
        alert(await res.text());
      }
    } catch (err) {
      console.error('Error checking in:', err);
    }
  };

  // Handle habit deletion
  const handleDeleteHabit = async habitId => {
    try {
      const res = await fetch(`http://localhost:8080/api/habits/${habitId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        setConfirmDeleteHabitId(null);
        await fetchHabits();
      } else {
        alert('Failed to delete habit.');
      }
    } catch (err) {
      console.error('Error deleting habit:', err);
    }
  };

  // Create a new habit
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
        await fetchHabits();
      } else {
        alert('Failed to create habit.');
      }
    } catch (err) {
      console.error('Error creating habit:', err);
    }
  };

  // Open edit modal and preload reminder time
  const openEditModal = habit => {
    setEditHabit({
      ...habit,
      reminderTime: reminderTimes[habit.id] || '',
    });
  };

  // Handle changes in edit modal
  const handleEditChange = (field, value) => {
    setEditHabit(prev => ({ ...prev, [field]: value }));
  };

  // Submit habit edits to backend
  const submitEditHabit = async () => {
    if (!editHabit.title.trim()) {
      alert('Title cannot be empty.');
      return;
    }
    if (isNaN(editHabit.targetDays) || editHabit.targetDays <= 0) {
      alert('Please enter a valid number for target days.');
      return;
    }

    try {
      const res = await fetch(`http://localhost:8080/api/habits/${editHabit.id}/edit`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          title: editHabit.title.trim(),
          targetDays: parseInt(editHabit.targetDays, 10),
          completed: editHabit.completed,
          reminderTime: editHabit.reminderTime?.trim() || null,
        }),
      });

      if (res.ok) {
        setEditHabit(null);
        await fetchHabits();
      } else {
        alert('Failed to edit habit.');
      }
    } catch (err) {
      console.error('Error editing habit:', err);
    }
  };

  // Toggle daily reminder checkbox
  const toggleReminder = habitId => {
    const current = reminderTimes[habitId] || '';
    if (current) {
      setReminderTimes(prev => ({ ...prev, [habitId]: '' }));
      updateReminder(habitId, '');
    } else {
      const defaultTime = '08:00';
      setReminderTimes(prev => ({ ...prev, [habitId]: defaultTime }));
      updateReminder(habitId, defaultTime);
    }
  };

  // Update reminder on server
  const updateReminder = async (habitId, timeStr) => {
    try {
      const res = await fetch(`http://localhost:8080/api/habits/${habitId}/edit`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          reminderTime: timeStr || null,
        }),
      });
      if (!res.ok) {
        alert('Failed to update reminder.');
      }
    } catch (err) {
      console.error('Error updating reminder:', err);
    }
  };

  // Format date helper
  const formatDate = dateStr => {
    return new Date(dateStr).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="logo-container">
          <span className="welcome-text">Welcome to</span>
          <img src={logo} alt="HabitForge Logo" className="dashboard-logo" />
        </div>

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
        <section className="habit-controls">
          <div className="habit-form">
            <input
              type="text"
              placeholder="Habit title"
              value={newHabitTitle}
              onChange={e => setNewHabitTitle(e.target.value)}
            />
            <input
              type="number"
              placeholder="Target days"
              value={newHabitTargetDays}
              onChange={e => setNewHabitTargetDays(e.target.value)}
            />
            <button className="create-habit-button" onClick={handleCreateHabit}>
              Create Habit
            </button>
          </div>

          <div className="sort-filter-controls">
            <label>
              Sort by:&nbsp;
              <select value={sortBy} onChange={e => setSortBy(e.target.value)}>
                <option value="startdate">Start Date</option>
                <option value="streak">Streak (High to Low)</option>
                <option value="completed">Completed Status</option>
              </select>
            </label>

            <label>
              Filter:&nbsp;
              <select value={filterByStatus} onChange={e => setFilterByStatus(e.target.value)}>
                <option value="all">All</option>
                <option value="active">Active</option>
                <option value="completed">Completed</option>
              </select>
            </label>
          </div>
        </section>

        <section className="habit-list">
          {filteredHabits.length === 0 ? (
            <p>No habits found. Add one above!</p>
          ) : (
            filteredHabits.map(habit => {
              const lastCheckInDate = habit.lastCheckInDate;
              const hasCheckedInToday = lastCheckInDate === today;

              return (
                <div key={habit.id} className="habit-card">
                  <h3>{habit.title}</h3>
                  <p>
                    Streak: {habit.currentStreak} / {habit.targetDays} days
                    {habit.completed && <span className="completed-badge">✓ Completed!</span>}
                  </p>

                  <div className="progress-bar">
                    <div
                      className="progress-fill"
                      style={{
                        width: `${Math.min((habit.currentStreak / habit.targetDays) * 100, 100)}%`,
                      }}
                    ></div>
                  </div>

                  <p className="last-checkin">
                    Last Check-In: {lastCheckInDate ? formatDate(lastCheckInDate) : 'Not yet'}
                  </p>

                  {/* Reminder toggle */}
                  <label className="reminder-toggle">
                    <input
                      type="checkbox"
                      checked={Boolean(reminderTimes[habit.id])}
                      onChange={() => toggleReminder(habit.id)}
                      disabled={habit.completed}
                    />
                    Remind me daily
                  </label>

                  {/* Edit & Delete Buttons */}
                  <div className="habit-card-buttons">
                    <button
                      className="checkin-button"
                      onClick={() => handleCheckIn(habit.id)}
                      disabled={hasCheckedInToday || habit.completed}
                    >
                      {habit.completed
                        ? 'Completed'
                        : hasCheckedInToday
                        ? 'Checked In Today'
                        : 'Check In'}
                    </button>
                    <button className="edit-button" onClick={() => openEditModal(habit)}>
                      Edit
                    </button>
                    <button className="delete-button" onClick={() => setConfirmDeleteHabitId(habit.id)}>
                      Delete
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </section>
      </main>

      {/* Edit Habit Modal */}
      {editHabit && (
        <div className="modal-overlay" onClick={() => setEditHabit(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <h2>Edit Habit</h2>
            <label>
              Title:
              <input
                type="text"
                value={editHabit.title}
                onChange={e => handleEditChange('title', e.target.value)}
              />
            </label>
            <label>
              Target Days:
              <input
                type="number"
                value={editHabit.targetDays}
                onChange={e => handleEditChange('targetDays', e.target.value)}
              />
            </label>
            <label>
              Completed:
              <input
                type="checkbox"
                checked={editHabit.completed}
                onChange={e => handleEditChange('completed', e.target.checked)}
              />
            </label>
            <label>
              Reminder Time:
              <input
                type="time"
                value={editHabit.reminderTime || ''}
                onChange={e => handleEditChange('reminderTime', e.target.value)}
                disabled={editHabit.completed}
              />
            </label>

            <div className="modal-buttons">
              <button onClick={submitEditHabit}>Save</button>
              <button onClick={() => setEditHabit(null)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      {/* Confirm Delete Modal */}
      {confirmDeleteHabitId && (
        <div className="modal-overlay" onClick={() => setConfirmDeleteHabitId(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <h3>Are you sure you want to delete this habit?</h3>
            <div className="modal-buttons">
              <button onClick={() => handleDeleteHabit(confirmDeleteHabitId)}>Yes, Delete</button>
              <button onClick={() => setConfirmDeleteHabitId(null)}>Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Dashboard;




















