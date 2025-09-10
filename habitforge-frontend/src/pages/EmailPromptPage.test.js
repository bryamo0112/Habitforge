import '@testing-library/jest-dom';
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import EmailPromptPage from './EmailPromptPage';

describe('EmailPromptPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('renders email input and submit button', () => {
    render(<EmailPromptPage />);
    expect(screen.getByPlaceholderText(/enter a valid email address/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /submit email/i })).toBeInTheDocument();
  });

  test('shows session expired message if token is missing', async () => {
    render(<EmailPromptPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter a valid email address/i), { target: { value: 'test@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /submit email/i }));

    expect(await screen.findByText(/session expired/i)).toBeInTheDocument();
  });

  test('shows server error message on failed response', async () => {
    localStorage.setItem('token', 'fake.jwt.token');
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: false,
        json: () => Promise.resolve({ message: 'Invalid email format' }),
      })
    );

    render(<EmailPromptPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter a valid email address/i), { target: { value: 'bad-email' } });
    fireEvent.click(screen.getByRole('button', { name: /submit email/i }));

    expect(await screen.findByText(/invalid email format/i)).toBeInTheDocument();
  });

  test('shows network/server error on fetch rejection', async () => {
    localStorage.setItem('token', 'fake.jwt.token');
    global.fetch = jest.fn(() => Promise.reject(new Error('Network error')));

    render(<EmailPromptPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter a valid email address/i), { target: { value: 'test@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /submit email/i }));

    expect(await screen.findByText(/server error while submitting email/i)).toBeInTheDocument();
  });

  test('successful submission displays info message and navigates', async () => {
    jest.useFakeTimers();

    localStorage.setItem('token', 'fake.jwt.token');
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      })
    );

    render(<EmailPromptPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter a valid email address/i), { target: { value: 'test@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /submit email/i }));

    expect(await screen.findByText(/verification code sent/i)).toBeInTheDocument();
    expect(global.mockedNavigate).toHaveBeenCalledWith('/verify-code', { state: { email: 'test@example.com' } });

    jest.useRealTimers();
  });

  test('clears messages when input changes', async () => {
    render(<EmailPromptPage />);
    const input = screen.getByPlaceholderText(/enter a valid email address/i);

    // set error and info messages
    fireEvent.change(input, { target: { value: 'test@example.com' } });
    fireEvent.change(input, { target: { value: 'changed@example.com' } });

    expect(screen.queryByText(/session expired/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/verification code sent/i)).not.toBeInTheDocument();
  });
});
