import React, { useState } from 'react';
import profileIcon from './assets/profiledef.jpg';
import './ProfilePicturePrompt.css';
import { useNavigate } from 'react-router-dom';

function ProfilePicturePrompt({ user, setUser }) {
  // user: object containing username, token, etc.
  // setUser: function to update user state after upload

  const [showModal, setShowModal] = useState(false);
  const [selectedImage, setSelectedImage] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const navigate = useNavigate();

  const handleYesClick = () => {
    setShowModal(true);
  };

  const handleNoClick = async () => {
    // User opts not to upload a profile picture
    // Inform backend user has been prompted to avoid prompt on reload
    if (user?.username) {
      try {
        await fetch(`/api/users/${user.username}/mark-prompted`, {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token')}`,
          },
        });

        // Update local user state to avoid prompt again
        setUser(prev => ({ ...prev, hasBeenPromptedForProfilePic: true }));

        // Delay navigation to ensure state updates before redirect
        setTimeout(() => {
          navigate('/dashboard');
        }, 100);
      } catch (err) {
        console.error('Error marking user as prompted:', err);
      }
    }

    // Proceed without profile pic
    setShowModal(false);
    setSelectedImage(null);
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedImage(file);
      setErrorMsg('');
    }
  };

  const handleUpload = async () => {
    if (!selectedImage) {
      setErrorMsg('Please select an image to upload.');
      return;
    }

    setUploading(true);
    setErrorMsg('');

    try {
      const formData = new FormData();
      formData.append('image', selectedImage);

      const res = await fetch(`/api/users/${user.username}/upload-profile-picture`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        body: formData,
      });

      if (!res.ok) {
        let errorMsg = 'Failed to upload profile picture.';
        try {
          const data = await res.json();
          errorMsg = data.error || errorMsg;
        } catch (_) {
          const text = await res.text(); // fallback to plain text
          errorMsg = text || errorMsg;
        }
        throw new Error(errorMsg);
      }

      // Successfully uploaded, update user profile picture in state
      const updatedUser = await res.json();
      setUser(updatedUser);

      // Delay navigation to ensure state updates before redirect
      setTimeout(() => {
        navigate('/dashboard');
      }, 100);

      setShowModal(false);
      setSelectedImage(null);
    } catch (error) {
      console.error('Upload error:', error);
      setErrorMsg(error.message || 'Error uploading image.');
    } finally {
      setUploading(false);
    }
  };

  const handleCancel = () => {
    setSelectedImage(null);
    setShowModal(false);
    setErrorMsg('');
  };

  return (
    <div className="profile-prompt-container">
      <img
        src={
          selectedImage
            ? URL.createObjectURL(selectedImage)
            : user?.profilePicture
            ? `data:${user.profilePictureContentType};base64,${user.profilePicture}`
            : profileIcon
        }
        alt="Profile Preview"
        className="profile-picture"
      />
      <p className="profile-question">
        Do you want to add a profile picture to your account{user?.username ? `, ${user.username}` : ''}?
      </p>
      <div className="profile-button-group">
        <button onClick={handleYesClick} disabled={uploading}>Yes</button>
        <button onClick={handleNoClick} disabled={uploading}>No</button>
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Select a profile picture</h3>

            {selectedImage && (
              <img
                src={URL.createObjectURL(selectedImage)}
                alt="Selected Profile Preview"
                className="modal-profile-preview"
              />
            )}

            <input
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              disabled={uploading}
            />

            {errorMsg && <div className="error-message">{errorMsg}</div>}

            <div className="modal-buttons">
              <button onClick={handleUpload} disabled={uploading}>
                {uploading ? 'Uploading...' : 'Upload'}
              </button>
              <button onClick={handleCancel} disabled={uploading}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ProfilePicturePrompt;






