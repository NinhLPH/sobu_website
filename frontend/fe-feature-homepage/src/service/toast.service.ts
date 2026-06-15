import { toast, ToastOptions } from 'react-toastify';

export const ToastService = {
    success: (message: string, options?: ToastOptions) => {
        toast.success(message, options);
    },
    error: (message: string, options?: ToastOptions) => {
        toast.error(message, options);
    },
    info: (message: string, options?: ToastOptions) => {
        toast.info(message, options);
    },
    warning: (message: string, options?: ToastOptions) => {
        toast.warning(message, options);
    },
    show: (message: string, options?: ToastOptions) => {
        toast(message, options);
    }
};
