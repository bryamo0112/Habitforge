import React from 'react';
import { render, screen, fireEvent} from '@testing-library/react';
import ResetPasswordPage from './ResetPasswordPage';

// Mock useLocation locally for this test
const mockedUseLocation = jest.fn();
jest.mock('react-router-dom', () => ({
  __esModule: true,
  useNavigate: () => global.mockedNavigate,
  useLocation: () => mockedUseLocation(),
}));

beforeEach(() => {
  jest.clearAllMocks();
  global.fetch = jest.fn();
  mockedUseLocation.mockReturnValue({ state: {} });
});

describe('ResetPasswordPage', () => {
  it('renders email, code, and new password inputs and submit button', () => {
    render(<ResetPasswordPage />);
    
    expect(screen.getByPlaceholderText(/email address/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/reset code/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/new password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /reset password/i })).toBeInTheDocument();
  });

  it('prefills email and code from location.state', () => {
    mockedUseLocation.mockReturnValueOnce({ state: { email: 'test@example.com', code: '123456' } });
    render(<ResetPasswordPage />);

    expect(screen.getByDisplayValue('test@example.com')).toBeInTheDocument();
    expect(screen.getByDisplayValue('123456')).toBeInTheDocument();
  });

it('successful password reset shows message and navigates', async () => {
  jest.useFakeTimers(); // enable fake timers

  global.fetch.mockResolvedValueOnce({ ok: true });

  render(<ResetPasswordPage />);

  fireEvent.change(screen.getByPlaceholderText(/email address/i), { target: { value: 'test@example.com' } });
  fireEvent.change(screen.getByPlaceholderText(/reset code/i), { target: { value: '123456' } });
  fireEvent.change(screen.getByPlaceholderText(/new password/i), { target: { value: 'password123' } });

  fireEvent.click(screen.getByRole('button', { name: /reset password/i }));

  // Wait for message to appear
  const infoMessage = await screen.findByText(/password reset successful/i);
  expect(infoMessage).toBeInTheDocument();

  // Fast-forward the setTimeout
  jest.runAllTimers();

  expect(global.mockedNavigate).toHaveBeenCalledWith('/');

  jest.useRealTimers(); // restore real timers
});


  it('shows JSON error from server', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: false,
      text: async () => JSON.stringify({ message: 'Invalid code' }),
    });

    render(<ResetPasswordPage />);

    fireEvent.change(screen.getByPlaceholderText(/email address/i), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByPlaceholderText(/reset code/i), { target: { value: 'wrongcode' } });
    fireEvent.change(screen.getByPlaceholderText(/new password/i), { target: { value: 'password123' } });

    fireEvent.click(screen.getByRole('button', { name: /reset password/i }));

    const errorMessage = await screen.findByText(/invalid code/i);
    expect(errorMessage).toBeInTheDocument();
  });

  it('shows plain text error from server', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: false,
      text: async () => 'Server is down',
    });

    render(<ResetPasswordPage />);

    fireEvent.change(screen.getByPlaceholderText(/email address/i), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByPlaceholderText(/reset code/i), { target: { value: '123456' } });
    fireEvent.change(screen.getByPlaceholderText(/new password/i), { target: { value: 'password123' } });

    fireEvent.click(screen.getByRole('button', { name: /reset password/i }));

    const errorMessage = await screen.findByText(/server is down/i);
    expect(errorMessage).toBeInTheDocument();
  });

  it('shows network/server error on fetch rejection', async () => {
    global.fetch.mockRejectedValueOnce(new Error('Network error'));

    render(<ResetPasswordPage />);

    fireEvent.change(screen.getByPlaceholderText(/email address/i), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByPlaceholderText(/reset code/i), { target: { value: '123456' } });
    fireEvent.change(screen.getByPlaceholderText(/new password/i), { target: { value: 'password123' } });

    fireEvent.click(screen.getByRole('button', { name: /reset password/i }));

    const errorMessage = await screen.findByText(/server error while resetting password/i);
    expect(errorMessage).toBeInTheDocument();
  });

  it('toggles password visibility when eye icon clicked', () => {
    render(<ResetPasswordPage />);

    const passwordInput = screen.getByPlaceholderText(/new password/i);
    const toggleIcon = screen.getByAltText(/show password/i);

    expect(passwordInput).toHaveAttribute('type', 'password');

    fireEvent.click(toggleIcon);
    expect(passwordInput).toHaveAttribute('type', 'text');

    fireEvent.click(toggleIcon);
    expect(passwordInput).toHaveAttribute('type', 'password');
  });
});




