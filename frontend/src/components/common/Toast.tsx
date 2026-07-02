import { ToastContainer, Slide } from 'react-toastify';
// @ts-ignore
import 'react-toastify/dist/ReactToastify.css';

export default function Toast() {
    return (
        <ToastContainer
            position="bottom-right"
            autoClose={1500}
            limit={5}
            hideProgressBar={false}
            newestOnTop={true}
            closeOnClick
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
            theme="light"
            transition={Slide}
            toastStyle={{
                borderRadius: '8px',
                boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                fontFamily: 'inherit'
            }}
        />
    );
}