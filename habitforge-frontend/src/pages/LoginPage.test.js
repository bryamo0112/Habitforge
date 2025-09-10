import '@testing-library/jest-dom';
import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import LoginPage from './LoginPage';

describe('LoginPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('renders login form inputs and button', () => {
    render(<LoginPage setUser={jest.fn()} />);
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Login$/ })).toBeInTheDocument();
  });

  test('shows error when login fails', async () => {
    global.fetch = jest.fn().mockResolvedValueOnce({
      ok: false,
      json: async () => ({ error: 'Invalid credentials' }),
    });

    render(<LoginPage setUser={jest.fn()} />);
    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'baduser' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: /^Login$/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid credentials');
    expect(localStorage.getItem('token')).toBeNull();
  });

  test('saves token and navigates to dashboard on successful login', async () => {
    const mockUser = {
      username: 'testuser',
      email: 'test@test.com',
      emailVerified: true,
      profilePicUrl: 'pic.png',
    };

    global.fetch = jest
      .fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ token: 'fake.jwt.token' }) })
      .mockResolvedValueOnce({ ok: true, json: async () => mockUser });

    const setUser = jest.fn();
    render(<LoginPage setUser={setUser} />);

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /^Login$/ }));

    await waitFor(() => expect(localStorage.getItem('token')).toBe('fake.jwt.token'));
    await waitFor(() => expect(setUser).toHaveBeenCalledWith(mockUser));
    await waitFor(() => expect(global.mockedNavigate).toHaveBeenCalledWith('/dashboard'));
  });

  test('navigates to verify-code when login returns 202', async () => {
    global.fetch = jest.fn().mockResolvedValueOnce({
      status: 202,
      json: async () => ({ partialToken: 'abc123' }),
    });

    render(<LoginPage setUser={jest.fn()} />);
    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'user' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'pass' } });
    fireEvent.click(screen.getByRole('button', { name: /^Login$/ }));

    await waitFor(() => expect(localStorage.getItem('pendingEmailVerification')).toBe('abc123'));
    await waitFor(() => expect(global.mockedNavigate).toHaveBeenCalledWith('/verify-code'));
  });

  test('shows error when server returns malformed/invalid token', async () => {
    global.fetch = jest
      .fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ token: 'invalid-token' }) });

    render(<LoginPage setUser={jest.fn()} />);
    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'user' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'pass' } });
    fireEvent.click(screen.getByRole('button', { name: /^Login$/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/invalid token/i);
    expect(localStorage.getItem('token')).toBeNull();
  });

  test('toggles password visibility when eye icon is clicked', () => {
    render(<LoginPage setUser={jest.fn()} />);
    const passwordInput = screen.getByLabelText(/password/i);
    const toggleIcon = screen.getByAltText(/show password/i);
    expect(passwordInput).toHaveAttribute('type', 'password');
    fireEvent.click(toggleIcon);
    expect(passwordInput).toHaveAttribute('type', 'text');
  });
});






