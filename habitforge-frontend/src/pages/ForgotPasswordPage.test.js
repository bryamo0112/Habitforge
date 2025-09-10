import '@testing-library/jest-dom';
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import ForgotPasswordPage from './ForgotPasswordPage';

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders email input and submit button', () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByPlaceholderText(/enter your email address/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send reset code/i })).toBeInTheDocument();
  });

  test('successful submission displays message and navigates', async () => {
    jest.useFakeTimers();
    global.fetch = jest.fn(() =>
      Promise.resolve({ ok: true, text: () => Promise.resolve('') })
    );

    render(<ForgotPasswordPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter your email address/i), { target: { value: 'test@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /send reset code/i }));

    expect(await screen.findByText(/a reset code has been sent/i)).toBeInTheDocument();
    jest.runAllTimers();
    expect(global.mockedNavigate).toHaveBeenCalledWith('/reset-password', { state: { email: 'test@example.com' } });
    jest.useRealTimers();
  });

  test('shows error message when server returns JSON error', async () => {
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: false,
        text: () => Promise.resolve(JSON.stringify({ message: 'Email not found' })),
      })
    );

    render(<ForgotPasswordPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter your email address/i), { target: { value: 'bad@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /send reset code/i }));

    expect(await screen.findByText(/email not found/i)).toBeInTheDocument();
  });

  test('shows error message when server returns plain text error', async () => {
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: false,
        text: () => Promise.resolve('Internal server error'),
      })
    );

    render(<ForgotPasswordPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter your email address/i), { target: { value: 'bad@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /send reset code/i }));

    expect(await screen.findByText(/internal server error/i)).toBeInTheDocument();
  });

  test('shows network/server error on fetch rejection', async () => {
    global.fetch = jest.fn(() => Promise.reject(new Error('Network error')));

    render(<ForgotPasswordPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter your email address/i), { target: { value: 'test@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /send reset code/i }));

    expect(await screen.findByText(/server error while sending reset code/i)).toBeInTheDocument();
  });
});
