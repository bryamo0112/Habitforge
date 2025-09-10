// App.Integration.test.js
import React from "react";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import App from "./App";

// --- Mock pages and components ---
jest.mock('./pages/LoginPage', () => {
  const React = require('react');
  return { __esModule: true, default: () => <div>Login Page</div> };
});
jest.mock('./pages/SignUpPage', () => {
  const React = require('react');
  return { __esModule: true, default: () => <div>Sign Up Page</div> };
});
jest.mock('./pages/ForgotPasswordPage', () => {
  const React = require('react');
  return { __esModule: true, default: () => <div>Forgot Password Page</div> };
});
jest.mock('./pages/VerificationCodePage', () => {
  const React = require('react');
  return { __esModule: true, default: () => <div>Verification Code Page</div> };
});
jest.mock('./pages/ResetPasswordPage', () => {
  const React = require('react');
  return { __esModule: true, default: () => <div>Reset Password Page</div> };
});
jest.mock('./pages/EmailLoginPage', () => {
  const React = require('react');
  return { __esModule: true, default: () => <div>Email Login Page</div> };
});
jest.mock('./pages/EmailPromptPage', () => {
  const React = require('react');
  return { __esModule: true, default: () => <div>Email Prompt Page</div> };
});
jest.mock('./pages/UsernamePromptPage', () => {
  const React = require('react');
  return { __esModule: true, default: () => <div>Username Prompt Page</div> };
});
jest.mock('./components/ProfilePicturePrompt', () => {
  const React = require('react');
  return { __esModule: true, default: () => <div>Profile Picture Prompt</div> };
});
jest.mock('./components/Dashboard', () => {
  const React = require('react');
  return { __esModule: true, default: () => <div>Dashboard</div> };
});

// --- Mock fetch & localStorage ---
global.fetch = jest.fn();
Object.defineProperty(window, "localStorage", {
  value: {
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn(),
  },
});

// --- Silence console warnings/errors ---
jest.spyOn(console, "warn").mockImplementation(() => {});
jest.spyOn(console, "error").mockImplementation(() => {});

describe("App routing integration", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders LoginPage on default route /", async () => {
    window.localStorage.getItem.mockReturnValue(null);
    render(<MemoryRouter initialEntries={["/"]}><App /></MemoryRouter>);
    expect(await screen.findByText(/login page/i)).toBeInTheDocument();
  });

  it("renders SignUpPage on /signup", async () => {
    render(<MemoryRouter initialEntries={["/signup"]}><App /></MemoryRouter>);
    expect(await screen.findByText(/sign up page/i)).toBeInTheDocument();
  });

  it("renders ForgotPasswordPage on /forgot-password", async () => {
    render(<MemoryRouter initialEntries={["/forgot-password"]}><App /></MemoryRouter>);
    expect(await screen.findByText(/forgot password page/i)).toBeInTheDocument();
  });

  it("redirects to LoginPage when accessing /dashboard without user", async () => {
    window.localStorage.getItem.mockReturnValue(null);
    render(<MemoryRouter initialEntries={["/dashboard"]}><App /></MemoryRouter>);
    expect(await screen.findByText(/login page/i)).toBeInTheDocument();
  });

  it("renders Dashboard when user is present", async () => {
    // Valid JWT with future expiration
    const mockToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                      btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 1000 })) +
                      ".signature";

    window.localStorage.getItem.mockReturnValue(mockToken);

    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ username: "testuser" }),
    });

    render(<MemoryRouter initialEntries={["/dashboard"]}><App /></MemoryRouter>);

    expect(await screen.findByText(/dashboard/i)).toBeInTheDocument();
  });

  it("redirects to LoginPage if token is expired", async () => {
    const expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                         btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) - 1000 })) +
                         ".signature";

    window.localStorage.getItem.mockReturnValue(expiredToken);

    render(<MemoryRouter initialEntries={["/dashboard"]}><App /></MemoryRouter>);

    expect(await screen.findByText(/login page/i)).toBeInTheDocument();
  });

  it("redirects to LoginPage if fetch returns 403", async () => {
    const mockToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                      btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 1000 })) +
                      ".signature";

    window.localStorage.getItem.mockReturnValue(mockToken);

    fetch.mockResolvedValueOnce({ ok: false, status: 403 });

    render(<MemoryRouter initialEntries={["/dashboard"]}><App /></MemoryRouter>);

    expect(await screen.findByText(/login page/i)).toBeInTheDocument();
  });
});


















