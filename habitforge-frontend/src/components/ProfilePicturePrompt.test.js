import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProfilePicturePrompt from './ProfilePicturePrompt';

describe('ProfilePicturePrompt', () => {
  let mockUser;
  let setUserMock;
  let navigateMock;

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <ProfilePicturePrompt user={mockUser} setUser={setUserMock} />
      </MemoryRouter>
    );

  beforeEach(() => {
    mockUser = { username: 'testuser', hasBeenPromptedForProfilePic: false };
    setUserMock = jest.fn();
    navigateMock = jest.fn();
    global.URL.createObjectURL = jest.fn(() => 'preview-url');
    jest.spyOn(require('react-router-dom'), 'useNavigate').mockReturnValue(navigateMock);
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ ...mockUser, profilePicture: 'newpic' }),
      })
    );
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('renders Yes/No buttons', () => {
    renderComponent();
    expect(screen.getByRole('button', { name: /Yes/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /No/i })).toBeInTheDocument();
  });

  test('opens modal when Yes is clicked', async () => {
    renderComponent();
    fireEvent.click(screen.getByRole('button', { name: /Yes/i }));

    expect(await screen.findByText(/Select a profile picture/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Upload/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
  });

  test('handles file selection', async () => {
    renderComponent();
    fireEvent.click(screen.getByRole('button', { name: /Yes/i }));

    const fileInput = await screen.findByLabelText(/Choose profile picture/i);
    const file = new File(['dummy'], 'photo.png', { type: 'image/png' });
    fireEvent.change(fileInput, { target: { files: [file] } });

    expect(fileInput.files[0]).toStrictEqual(file);
    expect(fileInput.files).toHaveLength(1);
  });

  test('shows error if upload clicked without selecting file', async () => {
    renderComponent();
    fireEvent.click(screen.getByRole('button', { name: /Yes/i }));
    fireEvent.click(screen.getByRole('button', { name: /Upload/i }));

    expect(await screen.findByText(/Please select an image to upload/i)).toBeInTheDocument();
  });

  // --- Fixed upload test ---
  test('uploads file successfully and navigates', async () => {
  // Ensure fetch returns the updated user
  fetch.mockImplementationOnce(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ ...mockUser, profilePicture: 'newpic' }),
    })
  );

  renderComponent();
  fireEvent.click(screen.getByRole('button', { name: /Yes/i }));

  const fileInput = await screen.findByLabelText(/Choose profile picture/i);
  const file = new File(['dummy'], 'photo.png', { type: 'image/png' });
  fireEvent.change(fileInput, { target: { files: [file] } });

  fireEvent.click(screen.getByRole('button', { name: /Upload/i }));

  // Wait for async state updates
  await waitFor(() =>
    expect(setUserMock).toHaveBeenCalledWith(
      expect.objectContaining({ profilePicture: 'newpic' })
    )
  );

  await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/dashboard'));
});

  test('shows JSON upload error', async () => {
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: false,
        json: () => Promise.resolve({ error: 'JSON upload error' }),
      })
    );

    renderComponent();
    fireEvent.click(screen.getByRole('button', { name: /Yes/i }));

    const fileInput = await screen.findByLabelText(/Choose profile picture/i);
    const file = new File(['dummy'], 'photo.png', { type: 'image/png' });
    fireEvent.change(fileInput, { target: { files: [file] } });

    fireEvent.click(screen.getByRole('button', { name: /Upload/i }));
    expect(await screen.findByText(/JSON upload error/i)).toBeInTheDocument();
  });

  test('shows plain text upload error', async () => {
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: false,
        json: () => Promise.reject('fail'),
        text: () => Promise.resolve('Plain text error'),
      })
    );

    renderComponent();
    fireEvent.click(screen.getByRole('button', { name: /Yes/i }));

    const fileInput = await screen.findByLabelText(/Choose profile picture/i);
    const file = new File(['dummy'], 'photo.png', { type: 'image/png' });
    fireEvent.change(fileInput, { target: { files: [file] } });

    fireEvent.click(screen.getByRole('button', { name: /Upload/i }));
    expect(await screen.findByText(/Plain text error/i)).toBeInTheDocument();
  });

  // --- Fixed No click test ---
  test('handles No click and navigates', async () => {
  // Mock fetch for PUT /mark-prompted
  fetch.mockImplementationOnce(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ success: true }),
    })
  );

  renderComponent();
  fireEvent.click(screen.getByRole('button', { name: /No/i }));

  // Wait for the async call to finish
  await waitFor(() => {
    // The first argument to setUserMock is the updater function
    const setUserArg = setUserMock.mock.calls[0][0];
    // Call it with the previous state to get the updated state
    const newState = setUserArg(mockUser);
    expect(newState).toEqual(
      expect.objectContaining({ hasBeenPromptedForProfilePic: true })
    );
  });

  await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/dashboard'));
});


  test('cancels modal correctly', async () => {
    renderComponent();
    fireEvent.click(screen.getByRole('button', { name: /Yes/i }));
    fireEvent.click(screen.getByRole('button', { name: /Cancel/i }));

    expect(screen.queryByText(/Select a profile picture/i)).not.toBeInTheDocument();
  });
});















