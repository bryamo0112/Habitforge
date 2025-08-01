import React, { useEffect, useState, useCallback } from 'react';
import defaultProfile from './assets/profiledef.jpg';
import logo from './assets/HabitForgeLogo.png';
import './Dashboard.css';
function triggerConfetti() {
  alert('🎉 Congrats on completing your habit! 🎉');
}

function Dashboard({ user, setUser, onLogout }) {
  const [habits, setHabits] = useState([]);
  const [newHabitTitle, setNewHabitTitle] = useState('');
  const [newHabitTargetDays, setNewHabitTargetDays] = useState('');
  const [sortBy, setSortBy] = useState('startdate');
  const [filterByStatus, setFilterByStatus] = useState('all');
  const [editHabit, setEditHabit] = useState(null);
  const [reminderTimes, setReminderTimes] = useState({});
  const [confirmDeleteHabitId, setConfirmDeleteHabitId] = useState(null);
  const [showPicModal, setShowPicModal] = useState(false);
  const [newProfilePic, setNewProfilePic] = useState(null);
  const [timeLeftByHabit, setTimeLeftByHabit] = useState({});
  const [timeModalHabitId, setTimeModalHabitId] = useState(null);
  const [selectedTime, setSelectedTime] = useState('08:00');


  const token = localStorage.getItem('token');

  const fetchHabits = useCallback(async () => {
  try {
    const url = new URL('/api/habits/sorted', window.location.origin);
    url.searchParams.append('sortBy', sortBy);

    const res = await fetch(url.toString(), {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok) {
      const data = await res.json();

      console.log("Fetched habits:", data);  // Debug: check reminderTime values

      setHabits(data);

      const times = {};
      data.forEach(habit => {
        times[habit.id] = habit.reminderTime || '';  // Ensure fallback empty string
      });
      setReminderTimes(times);

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

  useEffect(() => {
    function getTimeLeftString(reminderTimeStr) {
      if (!reminderTimeStr) return null;

      const [hourStr, minuteStr] = reminderTimeStr.split(':');
      if (hourStr == null || minuteStr == null) return null;

      const now = new Date();
      const reminder = new Date();
      reminder.setHours(parseInt(hourStr, 10));
      reminder.setMinutes(parseInt(minuteStr, 10));
      reminder.setSeconds(0);
      reminder.setMilliseconds(0);

      if (reminder <= now) {
        reminder.setDate(reminder.getDate() + 1);
      }

      const diffMs = reminder - now;
      const diffSeconds = Math.floor(diffMs / 1000);
      const hours = Math.floor(diffSeconds / 3600);
      const minutes = Math.floor((diffSeconds % 3600) / 60);
      const seconds = diffSeconds % 60;

      return `${String(hours).padStart(2, '0')}h ${String(minutes).padStart(2, '0')}m ${String(seconds).padStart(2, '0')}s until reminder`;
    }

    function updateTimeLeft() {
      const newTimeLeft = {};
      for (const [habitId, timeStr] of Object.entries(reminderTimes)) {
        newTimeLeft[habitId] = getTimeLeftString(timeStr);
      }
      setTimeLeftByHabit(newTimeLeft);
    }

    updateTimeLeft();
    const intervalId = setInterval(updateTimeLeft, 1000);
    return () => clearInterval(intervalId);
  }, [reminderTimes]);

  const filteredHabits = habits.filter(habit => {
    if (filterByStatus === 'active') return !habit.completed;
    if (filterByStatus === 'completed') return habit.completed;
    return true;
  });

  const today = new Date().toISOString().split('T')[0];

  const handleCheckIn = async habitId => {
    try {
      const res = await fetch(`/api/habits/${habitId}/check-in`, {
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
 const handleLogoutClick = () => {
    if (onLogout) onLogout();
  };
  const promptUserToPickTime = (habitId) => {
  const habit = habits.find(h => h.id === habitId);
  const existingTime = reminderTimes[habitId] || habit?.reminderTime || '08:00';
  setSelectedTime(existingTime);
  setTimeModalHabitId(habitId);
};


  const handleDeleteHabit = async habitId => {
    try {
      const res = await fetch(`/api/habits/${habitId}`, {
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

  const handleCreateHabit = async () => {
    const title = newHabitTitle.trim();
    const days = parseInt(newHabitTargetDays, 10);
    if (!title || isNaN(days) || days <= 0) {
      alert('Please enter a valid title and target days.');
      return;
    }

    try {
      const res = await fetch('/api/habits/create', {
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

  const openEditModal = habit => {
  setEditHabit({
    ...habit,
    reminderTime: reminderTimes[habit.id] || habit.reminderTime || '',
  });
};


  const handleEditChange = (field, value) => {
    setEditHabit(prev => ({ ...prev, [field]: value }));
  };

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
    const res = await fetch(`/api/habits/${editHabit.id}/edit`, {
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
      await fetchHabits();  // Refresh habits & reminderTimes from backend
      setEditHabit(null);   // Then close the modal
    } else {
      alert('Failed to edit habit.');
    }
  } catch (err) {
    console.error('Error editing habit:', err);
  }
};


const toggleReminder = habitId => {
  const current = reminderTimes[habitId] || '';
  const habit = habits.find(h => h.id === habitId);

  if (!current && !habit?.reminderTime) {
    promptUserToPickTime(habitId);
    return;
  }

  const preservedTime = habit?.reminderTime || '08:00';
  const newTime = current ? '' : preservedTime;

  setReminderTimes(prev => ({ ...prev, [habitId]: newTime }));
  updateReminder(habitId, newTime);
};


  const updateReminder = async (habitId, timeStr) => {
    try {
      const res = await fetch(`/api/habits/${habitId}/edit`, {
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
  


  const handleProfilePicUpload = async () => {
    if (!newProfilePic) {
      alert('Please select an image.');
      return;
    }

    const formData = new FormData();
    formData.append('image', newProfilePic);

    try {
      const res = await fetch(`/api/users/${user.username}/upload-profile-picture`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        body: formData,
      });

      if (res.ok) {
        const updatedUser = await res.json();
        console.log("Updated user after upload:", updatedUser);
        setUser(updatedUser);
        setShowPicModal(false);
        setNewProfilePic(null);
      } else {
        alert('Failed to upload image.');
      }
    } catch (err) {
      console.error('Error uploading profile picture:', err);
    }
  };

  const formatDate = (dateStr) => {
  const [year, month, day] = dateStr.split('-');
  const date = new Date(year, month - 1, day);
  return date.toLocaleDateString(undefined, {
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
            {user?.username?.trim() ? `Hello, ${user.username}` : 'Hello!'}
            <img
              src={
    user?.profilePicUrl
    ? user.profilePicUrl
    : defaultProfile
  }
              alt="Profile"
              className="dashboard-profile-pic"
              onClick={() => setShowPicModal(true)}
              style={{ cursor: 'pointer' }}
            />
          </div>
          <button className="logout-button" onClick={handleLogoutClick}>
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

            <label className='filter-label'>
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

                  <label className="reminder-toggle">
                    <input
                      type="checkbox"
                      checked={Boolean(reminderTimes[habit.id])}
                      onChange={() => toggleReminder(habit.id)}
                      disabled={habit.completed}
                    />
                    Remind me daily
                  </label>

                  {reminderTimes[habit.id] && !habit.completed && (
                    <p className="reminder-countdown" style={{ fontStyle: 'italic', fontSize: '0.9em' }}>
                      {timeLeftByHabit[habit.id]}
                    </p>
                  )}

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
                    <button
                      className="delete-button"
                      onClick={() => setConfirmDeleteHabitId(habit.id)}
                    >
                      Delete
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </section>
      </main>

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
        Remind me daily
        <input
          type="checkbox"
          checked={!!editHabit.reminderTime}
          onChange={e => {
            if (e.target.checked) {
              // Set default reminder time if none exists
              handleEditChange('reminderTime', editHabit.reminderTime || '08:00');
            } else {
              // Clear reminder time if unchecked
              handleEditChange('reminderTime', '');
            }
          }}
          disabled={editHabit.completed}
        />
      </label>

      <label>
        Reminder Time:
        <input
          type="time"
          value={editHabit.reminderTime || ''}
          onChange={e => handleEditChange('reminderTime', e.target.value)}
          disabled={editHabit.completed || !editHabit.reminderTime}
        />
      </label>

      <div className="modal-buttons">
        <button onClick={submitEditHabit}>Save</button>
        <button onClick={() => setEditHabit(null)}>Cancel</button>
      </div>
    </div>
  </div>
)}

{timeModalHabitId && (
  <div className="modal-overlay" onClick={() => setTimeModalHabitId(null)}>
    <div className="modal-content" onClick={e => e.stopPropagation()}>
      <h2>Set Reminder Time</h2>
      <label>
        Reminder Time:
        <input
          type="time"
          value={selectedTime}
          onChange={e => setSelectedTime(e.target.value)}
        />
      </label>
      <div className="modal-buttons">
        <button
          onClick={() => {
            updateReminder(timeModalHabitId, selectedTime);
            setReminderTimes(prev => ({
              ...prev,
              [timeModalHabitId]: selectedTime
            }));
            setTimeModalHabitId(null);
          }}
        >
          Save
        </button>
        <button onClick={() => setTimeModalHabitId(null)}>Cancel</button>
      </div>
    </div>
  </div>
)}



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

      {showPicModal && (
        <div className="modal-overlay" onClick={() => setShowPicModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <h3>Update Profile Picture</h3>
            {newProfilePic && (
              <img
                src={URL.createObjectURL(newProfilePic)}
                alt="Preview"
                style={{ width: '120px', height: '120px', borderRadius: '50%', marginBottom: '10px' }}
              />
            )}
            <input
              type="file"
              accept="image/*"
              onChange={e => setNewProfilePic(e.target.files?.[0] || null)}
            />
            <div className="modal-buttons">
              <button onClick={handleProfilePicUpload} disabled={!newProfilePic}>
                Upload
              </button>
              <button onClick={() => setShowPicModal(false)}>Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Dashboard;