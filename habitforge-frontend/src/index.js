import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import * as serviceWorkerRegistration from './serviceWorkerRegistration';
import reportWebVitals from './reportWebVitals';
import { BrowserRouter } from 'react-router-dom';  // <-- import BrowserRouter

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <BrowserRouter>        {/* <-- wrap App with BrowserRouter */}
      <App />
    </BrowserRouter>
  </React.StrictMode>
);

// Keep your service worker and reportWebVitals calls as is
serviceWorkerRegistration.unregister();
reportWebVitals();

