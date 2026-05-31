import React, { useState, useEffect } from 'react';
import { Mail, Lock, User, Phone, X, Eye, EyeOff, Loader2, CheckCircle2 } from 'lucide-react';
import { useAuthStore } from '../../store/useAuthStore';
import { AuthService } from '../../service/auth.service';

interface AuthModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export default function AuthModal({ isOpen, onClose }: AuthModalProps) {
    const { loginAction, isLoading, error, clearError } = useAuthStore();
    
    const [activeTab, setActiveTab] = useState<'login' | 'register'>('login');
    const [showPassword, setShowPassword] = useState(false);
    
    // Login form state
    const [loginEmail, setLoginEmail] = useState('');
    const [loginPassword, setLoginPassword] = useState('');
    
    // Register form state
    const [regName, setRegName] = useState('');
    const [regEmail, setRegEmail] = useState('');
    const [regPhone, setRegPhone] = useState('');
    const [regPassword, setRegPassword] = useState('');
    
    // Local flow state
    const [registerSuccess, setRegisterSuccess] = useState(false);
    const [localError, setLocalError] = useState<string | null>(null);
    const [isRegistering, setIsRegistering] = useState(false);

    // Reset error when switching tabs or closing modal
    useEffect(() => {
        clearError();
        setLocalError(null);
    }, [activeTab, isOpen, clearError]);

    if (!isOpen) return null;

    const handleLoginSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLocalError(null);

        if (!loginEmail || !loginPassword) {
            setLocalError('Vui lòng điền đầy đủ thông tin đăng nhập!');
            return;
        }

        try {
            await loginAction(loginEmail, loginPassword);
            // On success, close the modal
            onClose();
        } catch (err: any) {
            // Error is already managed in useAuthStore, but let's log it
            console.error('Login action failed:', err);
        }
    };

    const handleRegisterSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLocalError(null);
        setRegisterSuccess(false);

        if (!regName || !regEmail || !regPhone || !regPassword) {
            setLocalError('Vui lòng điền đầy đủ các thông tin đăng ký!');
            return;
        }

        setIsRegistering(true);
        try {
            const response = await AuthService.register({
                email: regEmail,
                password: regPassword,
                fullName: regName,
                phone: regPhone
            });
            
            // Check if success (usually returns register details)
            if (response) {
                setRegisterSuccess(true);
                // Clear form
                setRegName('');
                setRegEmail('');
                setRegPhone('');
                setRegPassword('');
            }
        } catch (err: any) {
            const msg = err?.response?.data?.message || err?.message || 'Đăng ký thất bại. Email hoặc Số điện thoại có thể đã tồn tại!';
            setLocalError(msg);
        } finally {
            setIsRegistering(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            {/* Backdrop Overlay */}
            <div 
                className="absolute inset-0 bg-inverse-surface/40 backdrop-blur-md cursor-pointer"
                onClick={onClose}
            />

            {/* Modal Dialog */}
            <div className="relative w-full max-w-md bg-surface-container-lowest rounded-3xl shadow-[0_30px_60px_-15px_rgba(14,48,78,0.15)] border border-surface-container/60 overflow-hidden transform transition-all duration-300 animate-in fade-in zoom-in-95">
                
                {/* Header Close button */}
                <button 
                    onClick={onClose}
                    className="absolute top-5 right-5 w-8 h-8 rounded-full flex items-center justify-center text-outline hover:text-error hover:bg-surface-container transition-all z-10"
                    title="Đóng"
                >
                    <X className="w-4 h-4" />
                </button>

                {/* Cover Banner Graphic with glassmorphism logo */}
                <div className="relative h-28 bg-gradient-to-r from-primary to-primary-container flex items-center justify-center overflow-hidden">
                    <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#fff_1px,transparent_1px)] [background-size:16px_16px]"></div>
                    <div className="z-10 text-center">
                        <span className="text-white text-3xl font-black tracking-widest bg-white/10 backdrop-blur-md px-6 py-1.5 rounded-2xl border border-white/20">
                            SOBU
                        </span>
                        <p className="text-white/80 text-[10px] uppercase font-bold tracking-wider mt-2.5">
                            MÔ HÌNH CUSTOM CAO CẤP
                        </p>
                    </div>
                </div>

                {/* Tabs selection */}
                {!registerSuccess && (
                    <div className="flex border-b border-surface-container/60">
                        <button
                            onClick={() => setActiveTab('login')}
                            className={`flex-1 py-4 text-sm font-black uppercase tracking-wider transition-all relative ${
                                activeTab === 'login' 
                                    ? 'text-primary' 
                                    : 'text-outline/70 hover:text-on-surface'
                            }`}
                        >
                            Đăng nhập
                            {activeTab === 'login' && (
                                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary animate-in fade-in" />
                            )}
                        </button>
                        <button
                            onClick={() => setActiveTab('register')}
                            className={`flex-1 py-4 text-sm font-black uppercase tracking-wider transition-all relative ${
                                activeTab === 'register' 
                                    ? 'text-primary' 
                                    : 'text-outline/70 hover:text-on-surface'
                            }`}
                        >
                            Đăng ký
                            {activeTab === 'register' && (
                                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary animate-in fade-in" />
                            )}
                        </button>
                    </div>
                )}

                {/* Content form container */}
                <div className="p-6">
                    {/* Display state errors */}
                    {(error || localError) && (
                        <div className="mb-4 p-3.5 rounded-xl bg-error/10 border border-error/20 text-error text-xs font-bold flex gap-2.5 items-start">
                            <span className="shrink-0 w-1.5 h-1.5 rounded-full bg-error mt-1.5" />
                            <span>{localError || error}</span>
                        </div>
                    )}

                    {registerSuccess ? (
                        /* SUCCESS REGISTER STATE */
                        <div className="py-8 flex flex-col items-center justify-center text-center animate-in fade-in slide-in-from-bottom-4">
                            <div className="w-16 h-16 rounded-full bg-primary-container/20 flex items-center justify-center text-primary mb-4">
                                <CheckCircle2 className="w-10 h-10 stroke-[2]" />
                            </div>
                            <h3 className="text-lg font-black text-on-surface mb-2">Đăng ký thành công!</h3>
                            <p className="text-xs text-outline/80 max-w-[280px] leading-relaxed mb-6">
                                Tài khoản của bạn đã được đăng ký thành công trên hệ thống SOBU. Hãy đăng nhập để tiếp tục.
                            </p>
                            <button
                                onClick={() => {
                                    setRegisterSuccess(false);
                                    setActiveTab('login');
                                }}
                                className="px-6 py-2.5 bg-primary text-white rounded-xl text-xs font-black uppercase tracking-widest shadow-md hover:scale-102 transition-transform"
                            >
                                Đăng nhập ngay
                            </button>
                        </div>
                    ) : activeTab === 'login' ? (
                        /* LOGIN FORM */
                        <form onSubmit={handleLoginSubmit} className="space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-1.5 pl-1">
                                    Địa chỉ Email
                                </label>
                                <div className="relative group">
                                    <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-outline/60 group-focus-within:text-primary transition-colors">
                                        <Mail className="w-4 h-4" />
                                    </div>
                                    <input
                                        type="email"
                                        value={loginEmail}
                                        onChange={(e) => setLoginEmail(e.target.value)}
                                        placeholder="name@example.com"
                                        disabled={isLoading}
                                        className="w-full bg-surface-container rounded-2xl pl-11 pr-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all placeholder:text-outline/40 text-on-surface"
                                        required
                                    />
                                </div>
                            </div>

                            <div>
                                <div className="flex justify-between items-center mb-1.5 pl-1">
                                    <label className="text-xs font-bold text-outline uppercase tracking-wider">
                                        Mật khẩu
                                    </label>
                                </div>
                                <div className="relative group">
                                    <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-outline/60 group-focus-within:text-primary transition-colors">
                                        <Lock className="w-4 h-4" />
                                    </div>
                                    <input
                                        type={showPassword ? "text" : "password"}
                                        value={loginPassword}
                                        onChange={(e) => setLoginPassword(e.target.value)}
                                        placeholder="••••••••"
                                        disabled={isLoading}
                                        className="w-full bg-surface-container rounded-2xl pl-11 pr-11 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all placeholder:text-outline/40 text-on-surface"
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword(!showPassword)}
                                        className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-outline/60 hover:text-on-surface transition-colors"
                                    >
                                        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                    </button>
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={isLoading}
                                className="w-full py-4 mt-2 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-lg shadow-primary/10 hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:pointer-events-none transition-all flex items-center justify-center gap-2"
                            >
                                {isLoading ? (
                                    <>
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                        <span>Đang xử lý...</span>
                                    </>
                                ) : (
                                    <span>Đăng nhập</span>
                                )}
                            </button>
                        </form>
                    ) : (
                        /* REGISTER FORM */
                        <form onSubmit={handleRegisterSubmit} className="space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-1.5 pl-1">
                                    Họ và tên
                                </label>
                                <div className="relative group">
                                    <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-outline/60 group-focus-within:text-primary transition-colors">
                                        <User className="w-4 h-4" />
                                    </div>
                                    <input
                                        type="text"
                                        value={regName}
                                        onChange={(e) => setRegName(e.target.value)}
                                        placeholder="Nguyễn Văn A"
                                        disabled={isRegistering}
                                        className="w-full bg-surface-container rounded-2xl pl-11 pr-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all placeholder:text-outline/40 text-on-surface"
                                        required
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-1.5 pl-1">
                                    Địa chỉ Email
                                </label>
                                <div className="relative group">
                                    <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-outline/60 group-focus-within:text-primary transition-colors">
                                        <Mail className="w-4 h-4" />
                                    </div>
                                    <input
                                        type="email"
                                        value={regEmail}
                                        onChange={(e) => setRegEmail(e.target.value)}
                                        placeholder="name@example.com"
                                        disabled={isRegistering}
                                        className="w-full bg-surface-container rounded-2xl pl-11 pr-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all placeholder:text-outline/40 text-on-surface"
                                        required
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-1.5 pl-1">
                                    Số điện thoại
                                </label>
                                <div className="relative group">
                                    <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-outline/60 group-focus-within:text-primary transition-colors">
                                        <Phone className="w-4 h-4" />
                                    </div>
                                    <input
                                        type="tel"
                                        value={regPhone}
                                        onChange={(e) => setRegPhone(e.target.value)}
                                        placeholder="0912345678"
                                        disabled={isRegistering}
                                        className="w-full bg-surface-container rounded-2xl pl-11 pr-4 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all placeholder:text-outline/40 text-on-surface"
                                        required
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-outline uppercase tracking-wider mb-1.5 pl-1">
                                    Mật khẩu
                                </label>
                                <div className="relative group">
                                    <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-outline/60 group-focus-within:text-primary transition-colors">
                                        <Lock className="w-4 h-4" />
                                    </div>
                                    <input
                                        type={showPassword ? "text" : "password"}
                                        value={regPassword}
                                        onChange={(e) => setRegPassword(e.target.value)}
                                        placeholder="••••••••"
                                        disabled={isRegistering}
                                        className="w-full bg-surface-container rounded-2xl pl-11 pr-11 py-3.5 text-xs font-semibold focus:ring-2 focus:ring-primary/20 outline-none border border-transparent focus:border-primary/20 transition-all placeholder:text-outline/40 text-on-surface"
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword(!showPassword)}
                                        className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-outline/60 hover:text-on-surface transition-colors"
                                    >
                                        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                    </button>
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={isRegistering}
                                className="w-full py-4 mt-2 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-lg shadow-primary/10 hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:pointer-events-none transition-all flex items-center justify-center gap-2"
                            >
                                {isRegistering ? (
                                    <>
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                        <span>Đang xử lý...</span>
                                    </>
                                ) : (
                                    <span>Đăng ký tài khoản</span>
                                )}
                            </button>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
}
