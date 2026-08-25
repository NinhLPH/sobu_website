import {StrictMode} from 'react';
import {createRoot} from 'react-dom/client';
import App from './App';
import {initializeTheme} from './store/useThemeStore';
// @ts-ignore
import './styles/index.css';

initializeTheme();

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <App />
    </StrictMode>,
);
