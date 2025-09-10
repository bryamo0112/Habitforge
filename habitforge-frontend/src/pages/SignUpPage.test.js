import '@testing-library/jest-dom';
import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import SignUpPage from './SignUpPage';

describe('SignUpPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    jest.useFakeTimers(); // for setTimeout
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  test('renders username and password inputs and sign up button', () => {
    render(<SignUpPage />);
    expect(screen.getByPlaceholderText(/username/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign up/i })).toBeInTheDocument();
  });

  test('shows error message when signup fails', async () => {
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: false,
        json: () => Promise.resolve({ message: 'Username already exists' }),
      })
    );

    render(<SignUpPage />);
    fireEvent.change(screen.getByPlaceholderText(/username/i), { target: { value: 'existingUser' } });
    fireEvent.change(screen.getByPlaceholderText(/password/i), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /sign up/i }));

    expect(await screen.findByText(/username already exists/i)).toBeInTheDocument();
  });

  test('shows server error on network failure', async () => {
    global.fetch = jest.fn(() => Promise.reject(new Error('Network error')));

    render(<SignUpPage />);
    fireEvent.change(screen.getByPlaceholderText(/username/i), { target: { value: 'newUser' } });
    fireEvent.change(screen.getByPlaceholderText(/password/i), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /sign up/i }));

    expect(await screen.findByText(/server error during signup/i)).toBeInTheDocument();
  });

  test('successful signup displays message and navigates after 2 seconds', async () => {
    global.fetch = jest.fn(() =>
      Promise.resolve({ ok: true })
    );

    render(<SignUpPage />);
    fireEvent.change(screen.getByPlaceholderText(/username/i), { target: { value: 'newUser' } });
    fireEvent.change(screen.getByPlaceholderText(/password/i), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /sign up/i }));

    // Check success message appears
    expect(await screen.findByText(/account created successfully/i)).toBeInTheDocument();

    // Fast-forward 2 seconds for redirect
    jest.advanceTimersByTime(2000);
    await waitFor(() => expect(global.mockedNavigate).toHaveBeenCalledWith('/profile-picture-prompt'));
  });

  test('toggles password visibility when eye icon is clicked', () => {
    render(<SignUpPage />);
    const passwordInput = screen.getByPlaceholderText(/password/i);
    const toggleIcon = screen.getByAltText(/show password/i);

    expect(passwordInput).toHaveAttribute('type', 'password');
    fireEvent.click(toggleIcon);
    expect(passwordInput).toHaveAttribute('type', 'text');
  });
});
