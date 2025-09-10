import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import VerificationCodePage from './VerificationCodePage';

// Mock useNavigate and useLocation locally
const mockedUseLocation = jest.fn();
jest.mock('react-router-dom', () => ({
  __esModule: true,
  useNavigate: () => global.mockedNavigate,
  useLocation: () => mockedUseLocation(),
}));

beforeEach(() => {
  jest.clearAllMocks();
  global.fetch = jest.fn();
  mockedUseLocation.mockReturnValue({ state: { email: 'test@example.com' } });
});

const mockSetUser = jest.fn();

describe('VerificationCodePage', () => {
  it('renders code input and verify button', () => {
    render(<VerificationCodePage setUser={mockSetUser} />);
    expect(screen.getByPlaceholderText(/enter the 6-digit code/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /verify code/i })).toBeInTheDocument();
  });

  it('prefills email from location.state', () => {
    render(<VerificationCodePage setUser={mockSetUser} />);
    expect(screen.getByText(/test@example.com/i)).toBeInTheDocument();
  });

  it('handles successful login verification and navigates correctly', async () => {
    global.fetch
      .mockResolvedValueOnce({ ok: true, json: async () => ({ token: 'new-token' }) }) // /verify-code
      .mockResolvedValueOnce({ ok: true, json: async () => ({ username: 'validuser', profilePicUrl: 'url' }) }); // /current

    render(<VerificationCodePage setUser={mockSetUser} />);
    fireEvent.change(screen.getByPlaceholderText(/enter the 6-digit code/i), { target: { value: '123456' } });
    fireEvent.click(screen.getByRole('button', { name: /verify code/i }));

    // Wait for DOM update
    await screen.findByRole('button', { name: /verify code/i });
    expect(localStorage.getItem('token')).toBe('new-token');
    expect(mockSetUser).toHaveBeenCalledWith({ username: 'validuser', profilePicUrl: 'url' });
    expect(global.mockedNavigate).toHaveBeenCalledWith('/dashboard');
  });

  it('handles reset password verification flow', async () => {
  // Mock location with purpose reset
  mockedUseLocation.mockReturnValue({ state: { email: 'test@example.com', purpose: 'reset' } });

  // Mock /verify-code fetch to succeed
  global.fetch.mockResolvedValueOnce({
    ok: true,
    json: async () => ({ token: 'ignored-token' }),
  });

  render(<VerificationCodePage setUser={mockSetUser} />);
  fireEvent.change(screen.getByPlaceholderText(/enter the 6-digit code/i), { target: { value: '654321' } });
  fireEvent.click(screen.getByRole('button', { name: /verify code/i }));

  // Wait for next tick so that useEffect and fetch are processed
  await screen.findByRole('button', { name: /verify code/i });

  // Check that navigation happened
  expect(global.mockedNavigate).toHaveBeenCalledWith('/reset-password', { state: { email: 'test@example.com' } });

  // Check pendingEmailVerification is cleared
  expect(localStorage.getItem('pendingEmailVerification')).toBeNull();
});


  it('shows error message if verification fails with JSON', async () => {
    global.fetch.mockResolvedValueOnce({ ok: false, json: async () => ({ error: 'Invalid code' }) });

    render(<VerificationCodePage setUser={mockSetUser} />);
    fireEvent.change(screen.getByPlaceholderText(/enter the 6-digit code/i), { target: { value: '000000' } });
    fireEvent.click(screen.getByRole('button', { name: /verify code/i }));

    expect(await screen.findByText(/invalid code/i)).toBeInTheDocument();
  });

  it('shows generic error if verification fails with non-JSON', async () => {
    global.fetch.mockResolvedValueOnce({ ok: false, json: async () => { throw new Error('No JSON'); } });

    render(<VerificationCodePage setUser={mockSetUser} />);
    fireEvent.change(screen.getByPlaceholderText(/enter the 6-digit code/i), { target: { value: '000000' } });
    fireEvent.click(screen.getByRole('button', { name: /verify code/i }));

    // Expect the component fallback error
    expect(await screen.findByText(/server error during verification/i)).toBeInTheDocument();
  });

  it('shows network/server error on fetch rejection', async () => {
    global.fetch.mockRejectedValueOnce(new Error('Network error'));

    render(<VerificationCodePage setUser={mockSetUser} />);
    fireEvent.change(screen.getByPlaceholderText(/enter the 6-digit code/i), { target: { value: '000000' } });
    fireEvent.click(screen.getByRole('button', { name: /verify code/i }));

    expect(await screen.findByText(/server error during verification/i)).toBeInTheDocument();
  });

  it('can resend verification code', async () => {
    global.fetch.mockResolvedValueOnce({ ok: true });

    render(<VerificationCodePage setUser={mockSetUser} />);
    fireEvent.click(screen.getByText(/didn’t get a code\? resend/i));

    expect(await screen.findByText(/a new verification code was sent/i)).toBeInTheDocument();
  });

  it('prevents resend if email is missing', async () => {
    mockedUseLocation.mockReturnValueOnce({ state: {} }); // no email

    render(<VerificationCodePage setUser={mockSetUser} />);
    fireEvent.click(screen.getByText(/didn’t get a code\? resend/i));

    expect(await screen.findByText(/server error while resending code/i)).toBeInTheDocument();
  });
});





