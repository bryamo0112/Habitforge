import React, { useState } from 'react';
import profileIcon from './assets/profiledef.jpg';
import './ProfilePicturePrompt.css';

function ProfilePicturePrompt({ username, onChoice }) {
  const [showModal, setShowModal] = useState(false);
  const [selectedImage, setSelectedImage] = useState(null);

  const handleYesClick = () => {
    setShowModal(true);
  };

  const handleNoClick = () => {
    onChoice(false, null);
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedImage(file);
    }
  };

  const handleUpload = () => {
    if (selectedImage) {
      onChoice(true, selectedImage);
      setShowModal(false);
    } else {
      alert("Please select an image to upload.");
    }
  };

  const handleCancel = () => {
    setSelectedImage(null);
    setShowModal(false);
  };

  return (
    <div className="profile-prompt-container">
      <img
        src={selectedImage ? URL.createObjectURL(selectedImage) : profileIcon}
        alt="Profile Preview"
        className="profile-picture"
      />
      <p className="profile-question">
        Do you want to add a profile picture to your account{username ? `, ${username}` : ''}?
      </p>
      <div className="profile-button-group">
        <button onClick={handleYesClick}>Yes</button>
        <button onClick={handleNoClick}>No</button>
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Select a profile picture</h3>

            {/* Added preview inside modal */}
            {selectedImage && (
              <img
                src={URL.createObjectURL(selectedImage)}
                alt="Selected Profile Preview"
                className="modal-profile-preview"
              />
            )}

            <input type="file" accept="image/*" onChange={handleFileChange} />
            <div className="modal-buttons">
              <button onClick={handleUpload}>Upload</button>
              <button onClick={handleCancel}>Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ProfilePicturePrompt;




