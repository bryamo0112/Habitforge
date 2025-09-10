import React from 'react';
import { render, screen, fireEvent, waitFor, act, within } from '@testing-library/react';
import Dashboard from './Dashboard';
import { MemoryRouter } from 'react-router-dom';

jest.useFakeTimers();

describe('Dashboard Component', () => {
  let mockUser;
  let setUserMock;
  let onLogoutMock;
  const token = 'fake-jwt-token';

  beforeEach(() => {
    mockUser = { username: 'testuser', profilePicUrl: null };
    setUserMock = jest.fn();
    onLogoutMock = jest.fn();
    localStorage.setItem('token', token);
    localStorage.clear(); // clear lastLoginTime

    global.fetch = jest.fn((url, options) => {
      if (url.includes('/api/habits/sorted')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 1, title: 'Test Habit', targetDays: 5, currentStreak: 2, completed: false, lastCheckInDate: new Date().toISOString().split('T')[0], reminderTime: '08:00' }
          ]),
        });
      }
      if (url.includes('/check-in')) return Promise.resolve({ ok: true });
      if (url.includes('/habits/create')) return Promise.resolve({ ok: true });
      if (url.includes('/habits/1/edit')) return Promise.resolve({ ok: true });
      if (url.includes('/habits/1')) return Promise.resolve({ ok: true });
      return Promise.resolve({ ok: true, json: () => Promise.resolve(mockUser) });
    });

    jest.spyOn(window, 'alert').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const renderDashboard = () =>
    render(
      <MemoryRouter>
        <Dashboard user={mockUser} setUser={setUserMock} onLogout={onLogoutMock} />
      </MemoryRouter>
    );

  test('renders welcome message and logout button', () => {
    renderDashboard();
    expect(screen.getByText(/Hello, testuser/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Logout/i })).toBeInTheDocument();
  });

  test('calls onLogout when logout button is clicked', () => {
    renderDashboard();
    fireEvent.click(screen.getByRole('button', { name: /Logout/i }));
    expect(onLogoutMock).toHaveBeenCalled();
  });

  test('fetches and displays habits', async () => {
    renderDashboard();
    expect(await screen.findByText(/Test Habit/i)).toBeInTheDocument();
    expect(screen.getByText(/Streak: 2 \/ 5 days/i)).toBeInTheDocument();
  });

  test('allows creating a new habit', async () => {
    jest.spyOn(Storage.prototype, 'getItem').mockImplementation(key => key === 'token' ? token : null);
    global.fetch.mockImplementation((url, options) => {
      if (url.includes('/api/habits/create')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ id: 1, title: 'New Habit', targetDays: 3 }),
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    renderDashboard();

    fireEvent.change(screen.getByPlaceholderText(/Habit title/i), { target: { value: 'New Habit' } });
    fireEvent.change(screen.getByPlaceholderText(/Target days/i), { target: { value: '3' } });
    fireEvent.click(screen.getByText(/Create Habit/i));

    // Split assertions into separate waitFor calls
    await waitFor(() => {
      const postCall = global.fetch.mock.calls.find(([url, options]) => url.includes('/api/habits/create') && options.method === 'POST');
      expect(postCall).toBeDefined();
    });

    await waitFor(() => {
      const postCall = global.fetch.mock.calls.find(([url, options]) => url.includes('/api/habits/create') && options.method === 'POST');
      expect(postCall[1]).toEqual(expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ Authorization: `Bearer ${token}` }),
        body: expect.any(String),
      }));
    });
  });

  test('handles check-in button', async () => {
  // Mock a habit NOT checked in today
  global.fetch.mockImplementationOnce(() =>
    Promise.resolve({
      ok: true,
      json: () =>
        Promise.resolve([
          {
            id: 1,
            title: 'Test Habit',
            targetDays: 5,
            currentStreak: 2,
            completed: false,
            lastCheckInDate: '2000-01-01', // not today
            reminderTime: '08:00',
          },
        ]),
    })
  );

  renderDashboard();

  const checkInButton = await screen.findByRole('button', { name: /Check In/i });
  expect(checkInButton).toBeEnabled();

  fireEvent.click(checkInButton);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/habits/1/check-in',
      expect.objectContaining({ method: 'POST' })
    );
  });
});


  test('toggles reminder and updates time', async () => {
    renderDashboard();
    const checkbox = await screen.findByRole('checkbox', { name: /Remind me daily/i });
    expect(checkbox).toBeChecked();

    fireEvent.click(checkbox); // uncheck
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/habits/1/edit', expect.objectContaining({
        method: 'PUT',
        body: expect.stringContaining('"reminderTime":null'),
      }));
    });
  });

  test('opens and closes edit modal', async () => {
    renderDashboard();
    fireEvent.click(await screen.findByRole('button', { name: /Edit/i }));

    expect(screen.getByText(/Edit Habit/i)).toBeInTheDocument();

    fireEvent.click(screen.getByText(/Cancel/i));
    expect(screen.queryByText(/Edit Habit/i)).not.toBeInTheDocument();
  });

  test('edits habit and submits changes', async () => {
  renderDashboard();

  // Open the edit modal
  fireEvent.click(await screen.findByRole('button', { name: /Edit/i }));

  // Wait for the modal content to appear using data-testid
  const modalContainer = await screen.findByTestId('edit-modal');

  // Title input
  const titleInput = within(modalContainer).getByLabelText(/Title:/i);
  fireEvent.change(titleInput, { target: { value: 'Updated Habit' } });

  // Target Days input
  const targetDaysInput = within(modalContainer).getByLabelText(/Target Days:/i);
  fireEvent.change(targetDaysInput, { target: { value: '10' } });

  // Completed checkbox
  const completedCheckbox = within(modalContainer).getByLabelText(/Completed:/i);
  fireEvent.click(completedCheckbox);

  // Reminder checkbox
  const reminderCheckbox = within(modalContainer).getByLabelText(/Remind me daily/i);
  fireEvent.click(reminderCheckbox);

  // Save changes
  fireEvent.click(within(modalContainer).getByText(/Save/i));

  // Assert PUT request was called
  await waitFor(() => {
    const putCall = global.fetch.mock.calls.find(
      ([url, options]) =>
        url.includes('/api/habits/1/edit') && options.method === 'PUT'
    );
    expect(putCall).toBeDefined();
  });

  // Assert PUT body contains updated title
  await waitFor(() => {
    const putCall = global.fetch.mock.calls.find(
      ([url, options]) =>
        url.includes('/api/habits/1/edit') && options.method === 'PUT'
    );
    expect(putCall[1].body).toContain('"title":"Updated Habit"');
  });

  // Assert modal is closed
  await waitFor(() => {
    expect(screen.queryByTestId('edit-modal')).not.toBeInTheDocument();
  });
});



  test('opens and closes delete modal', async () => {
    renderDashboard();
    fireEvent.click(await screen.findByRole('button', { name: /Delete/i }));

    expect(screen.getByText(/Are you sure you want to delete this habit\?/i)).toBeInTheDocument();
    fireEvent.click(screen.getByText(/Cancel/i));
    expect(screen.queryByText(/Are you sure you want to delete this habit\?/i)).not.toBeInTheDocument();
  });

  test('opens and closes profile picture modal', async () => {
    renderDashboard();
    const profilePic = screen.getByAltText('Profile');
    fireEvent.click(profilePic);

    expect(screen.getByText(/Update Profile Picture/i)).toBeInTheDocument();
    fireEvent.click(screen.getByText(/Cancel/i));
    expect(screen.queryByText(/Update Profile Picture/i)).not.toBeInTheDocument();
  });

  test('updates countdown timer', async () => {
    renderDashboard();
    act(() => {
      jest.advanceTimersByTime(2000);
    });

    const countdown = await screen.findByText(/until reminder/i);
    expect(countdown).toBeInTheDocument();
  });

  test('updates reminder time via modal', async () => {
    const mockHabits = [{ id: 1, title: 'Habit 1', currentStreak: 0, targetDays: 5, completed: false, lastCheckInDate: null, reminderTime: '' }];
    global.fetch = jest.fn((url, options) => {
      if (url.includes('/api/habits/sorted')) return Promise.resolve({ ok: true, json: () => Promise.resolve(mockHabits) });
      if (url.includes('/api/habits/1/edit') && options.method === 'PUT') return Promise.resolve({ ok: true });
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });

    renderDashboard({ user: { username: 'testuser' }, setUser: jest.fn() });

    const habitCheckbox = await screen.findByRole('checkbox', { name: /Remind me daily/i });
    fireEvent.click(habitCheckbox);

    const modal = await screen.findByTestId('time-modal');
    const timeInput = within(modal).getByLabelText(/Reminder Time/i);
    fireEvent.change(timeInput, { target: { value: '12:30' } });

    fireEvent.click(within(modal).getByText(/^Save$/i));

    await waitFor(() => {
      const putCall = global.fetch.mock.calls.find(([url, options]) => url.includes('/api/habits/1/edit') && options.method === 'PUT');
      expect(putCall).toBeDefined();
    });

    await waitFor(() => {
      const putCall = global.fetch.mock.calls.find(([url, options]) => url.includes('/api/habits/1/edit') && options.method === 'PUT');
      expect(putCall[1].body).toContain('"reminderTime":"12:30"');
    });
  });

});







