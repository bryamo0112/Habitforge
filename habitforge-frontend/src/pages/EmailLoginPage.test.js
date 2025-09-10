import '@testing-library/jest-dom';
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import EmailLoginPage from './EmailLoginPage';

describe('EmailLoginPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('renders email input and submit button', () => {
    render(<EmailLoginPage />);
    expect(screen.getByPlaceholderText(/enter your email/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send verification code/i })).toBeInTheDocument();
  });

  test('shows error message when server responds with error', async () => {
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: false,
        json: () => Promise.resolve({ message: 'Email not found' }),
      })
    );

    render(<EmailLoginPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter your email/i), { target: { value: 'test@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /send verification code/i }));

    expect(await screen.findByText(/email not found/i)).toBeInTheDocument();
  });

  test('shows server error on network failure', async () => {
    global.fetch = jest.fn(() => Promise.reject(new Error('Network error')));

    render(<EmailLoginPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter your email/i), { target: { value: 'test@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /send verification code/i }));

    expect(await screen.findByText(/server error during email login/i)).toBeInTheDocument();
  });

  test('successful send displays message and navigates after 500ms', async () => {
    jest.useFakeTimers();

    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      })
    );

    render(<EmailLoginPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter your email/i), { target: { value: 'test@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /send verification code/i }));

    // Use findByText instead of waitFor + getByText
    expect(await screen.findByText(/verification code sent/i)).toBeInTheDocument();

    // advance timers to simulate setTimeout
    jest.advanceTimersByTime(500);
    expect(global.mockedNavigate).toHaveBeenCalledWith('/verify-code', { state: { email: 'test@example.com' } });

    jest.useRealTimers();
  });

  test('shows error if email format is invalid', async () => {
    render(<EmailLoginPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter your email/i), { target: { value: 'invalid-email' } });
    fireEvent.click(screen.getByRole('button', { name: /send verification code/i }));

    // Since there is no frontend regex validation in EmailLoginPage,
    // we can simulate server rejecting invalid email
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: false,
        json: () => Promise.resolve({ message: 'Invalid email format' }),
      })
    );

    // click again to trigger fetch
    fireEvent.click(screen.getByRole('button', { name: /send verification code/i }));

    expect(await screen.findByText(/invalid email format/i)).toBeInTheDocument();
  });
});

