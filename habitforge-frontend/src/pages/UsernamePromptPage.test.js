import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import UsernamePromptPage from './UsernamePromptPage';

beforeEach(() => {
  localStorage.clear();
  jest.clearAllMocks();
  global.fetch = jest.fn();
  global.mockedNavigate = jest.fn();
});

describe('UsernamePromptPage', () => {
  it('renders username input and submit button', () => {
    render(<UsernamePromptPage />);
    expect(screen.getByPlaceholderText(/enter a unique username/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /save username/i })).toBeInTheDocument();
  });

  it('shows session expired error if token is missing', async () => {
    render(<UsernamePromptPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter a unique username/i), { target: { value: 'testuser' } });
    fireEvent.click(screen.getByRole('button', { name: /save username/i }));

    expect(await screen.findByText(/session expired/i)).toBeInTheDocument();
  });

  it('submits username and navigates to profile picture prompt if hasBeenPromptedForProfilePic is false', async () => {
    localStorage.setItem('token', 'valid-token');

    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ token: 'new-token', hasBeenPromptedForProfilePic: false }),
    });

    render(<UsernamePromptPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter a unique username/i), { target: { value: 'testuser' } });
    fireEvent.click(screen.getByRole('button', { name: /save username/i }));

    // Wait separately for token update
    await waitFor(() => expect(localStorage.getItem('token')).toBe('new-token'));

    // Wait separately for navigation
    await waitFor(() => expect(global.mockedNavigate).toHaveBeenCalledWith('/prompt-profile-picture'));
  });

  it('submits username and navigates to dashboard if hasBeenPromptedForProfilePic is true', async () => {
    localStorage.setItem('token', 'valid-token');

    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ token: 'new-token', hasBeenPromptedForProfilePic: true }),
    });

    render(<UsernamePromptPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter a unique username/i), { target: { value: 'testuser' } });
    fireEvent.click(screen.getByRole('button', { name: /save username/i }));

    // Wait separately for token update
    await waitFor(() => expect(localStorage.getItem('token')).toBe('new-token'));

    // Wait separately for navigation
    await waitFor(() => expect(global.mockedNavigate).toHaveBeenCalledWith('/dashboard'));
  });

  it('shows server JSON error message if fetch responds with JSON error', async () => {
    localStorage.setItem('token', 'valid-token');

    global.fetch.mockResolvedValueOnce({
      ok: false,
      json: async () => ({ message: 'Username already taken' }),
    });

    render(<UsernamePromptPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter a unique username/i), { target: { value: 'testuser' } });
    fireEvent.click(screen.getByRole('button', { name: /save username/i }));

    expect(await screen.findByText(/username already taken/i)).toBeInTheDocument();
  });

  it('shows generic error if fetch responds with non-JSON error', async () => {
    localStorage.setItem('token', 'valid-token');

    global.fetch.mockResolvedValueOnce({
      ok: false,
      json: async () => { throw new Error('No JSON'); },
    });

    render(<UsernamePromptPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter a unique username/i), { target: { value: 'testuser' } });
    fireEvent.click(screen.getByRole('button', { name: /save username/i }));

    expect(await screen.findByText(/username already taken or unauthorized/i)).toBeInTheDocument();
  });

  it('shows network/server error if fetch rejects', async () => {
    localStorage.setItem('token', 'valid-token');

    global.fetch.mockRejectedValueOnce(new Error('Network error'));

    render(<UsernamePromptPage />);
    fireEvent.change(screen.getByPlaceholderText(/enter a unique username/i), { target: { value: 'testuser' } });
    fireEvent.click(screen.getByRole('button', { name: /save username/i }));

    expect(await screen.findByText(/server error while submitting username/i)).toBeInTheDocument();
  });
});



