import '@testing-library/jest-dom';
import React from 'react';

// global mocked navigate
global.mockedNavigate = jest.fn();

// Fully mock react-router-dom for tests
jest.mock('react-router-dom', () => ({
  __esModule: true,
  // minimal mocks
  useNavigate: () => global.mockedNavigate,
  MemoryRouter: ({ children }) => <div>{children}</div>,
  Link: ({ children }) => <div>{children}</div>,
}));




