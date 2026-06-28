import React, {useCallback, useEffect, useRef, useState} from 'react';
import {Mail, Lock, User, Phone, Eye, EyeOff, Loader2} from 'lucide-react';
import {useAuthStore} from '../store/useAuthStore';
import {useNavigate} from 'react-router-dom';

const GOOGLE_SCRIPT_ID = 'google-identity-services-script';
const GOOGLE_SCRIPT_SRC = 'https://accounts.google.com/gsi/client';

const loadGoogleScript = (): Promise<void> => {
    if (window.google?.accounts?.id) {
        return Promise.resolve();
    }

    const existingScript = document.getElementById(GOOGLE_SCRIPT_ID) as HTMLScriptElement | null;
    if (existingScript) {
        return new Promise((resolve, reject) => {
            existingScript.addEventListener('load', () => resolve(), {once: true});
            existingScript.addEventListener('error', () => reject(new Error('Google Identity Services failed to load.')), {once: true});
        });
    }

    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.id = GOOGLE_SCRIPT_ID;
        script.src = GOOGLE_SCRIPT_SRC;
        script.async = true;
        script.defer = true;
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('Google Identity Services failed to load.'));
        document.head.appendChild(script);
    });
};

export default function LoginPage() {
    const navigate = useNavigate();
    const {
        loginAction,
        registerAction,
        googleLoginAction,
        isLoading,
        error,
        clearError,
        isAuthenticated
    } = useAuthStore();

    const googleButtonRef = useRef<HTMLDivElement | null>(null);
    const googleClientId = process.env.REACT_APP_GOOGLE_CLIENT_ID;

    const [activeTab, setActiveTab] = useState<'login' | 'register'>('login');
    const [showPassword, setShowPassword] = useState(false);
    const [isGoogleReady, setIsGoogleReady] = useState(false);

    const [loginEmail, setLoginEmail] = useState('');
    const [loginPassword, setLoginPassword] = useState('');

    const [regName, setRegName] = useState('');
    const [regEmail, setRegEmail] = useState('');
    const [regPhone, setRegPhone] = useState('');
    const [regPassword, setRegPassword] = useState('');
    const [regConfirmPassword, setRegConfirmPassword] = useState('');
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const [localError, setLocalError] = useState<string | null>(null);

    useEffect(() => {
        if (isAuthenticated) {
            navigate('/');
        }
    }, [isAuthenticated, navigate]);

    useEffect(() => {
        clearError();
        setLocalError(null);
    }, [activeTab, clearError]);

    const handleGoogleCredential = useCallback(async (response: GoogleCredentialResponse) => {
        setLocalError(null);
        clearError();

        if (!response.credential) {
            setLocalError('Không nhận được mã xác thực từ Google.');
            return;
        }

        try {
            await googleLoginAction(response.credential);
            navigate('/');
        } catch {
            // The store exposes the backend message through `error`.
        }
    }, [clearError, googleLoginAction, navigate]);

    useEffect(() => {
        if (!googleClientId || !googleButtonRef.current) {
            setIsGoogleReady(false);
            return;
        }

        let cancelled = false;
        setIsGoogleReady(false);

        loadGoogleScript()
            .then(() => {
                if (cancelled || !window.google?.accounts?.id || !googleButtonRef.current) {
                    return;
                }

                const buttonWidth = Math.min(352, googleButtonRef.current.clientWidth || 352);

                window.google.accounts.id.initialize({
                    client_id: googleClientId,
                    callback: handleGoogleCredential
                });
                googleButtonRef.current.innerHTML = '';
                window.google.accounts.id.renderButton(googleButtonRef.current, {
                    theme: 'outline',
                    size: 'large',
                    type: 'standard',
                    shape: 'pill',
                    text: activeTab === 'register' ? 'signup_with' : 'signin_with',
                    width: buttonWidth
                });
                setIsGoogleReady(true);
            })
            .catch(() => {
                if (!cancelled) {
                    setLocalError('Không thể tải đăng nhập Google lúc này.');
                }
            });

        return () => {
            cancelled = true;
        };
    }, [activeTab, googleClientId, handleGoogleCredential]);

    const handleMissingGoogleClientId = () => {
        clearError();
        setLocalError('Chưa cấu hình REACT_APP_GOOGLE_CLIENT_ID cho đăng nhập Google.');
    };

    const handleLoginSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLocalError(null);

        if (!loginEmail || !loginPassword) {
            setLocalError('Vui lòng điền đầy đủ thông tin đăng nhập!');
            return;
        }

        try {
            await loginAction(loginEmail.trim(), loginPassword);
            navigate('/');
        } catch {
            // The store exposes the backend message through `error`.
        }
    };

    const handleRegisterSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLocalError(null);

        if (!regName || !regEmail || !regPhone || !regPassword || !regConfirmPassword) {
            setLocalError('Vui lòng điền đầy đủ các thông tin đăng ký!');
            return;
        }

        if (regPassword !== regConfirmPassword) {
            setLocalError('Mật khẩu nhập lại không khớp!');
            return;
        }

        try {
            await registerAction({
                email: regEmail.trim(),
                password: regPassword,
                fullName: regName.trim(),
                phone: regPhone.trim()
            });
            navigate('/');
        } catch {
            // The store exposes the backend message through `error`.
        }
    };

    const renderGoogleLogin = () => (
        <div className="mt-5 space-y-3">
            <div className="flex items-center gap-3">
                <div className="h-px flex-1 bg-surface-container"/>
                <span className="text-[11px] font-bold uppercase text-outline">Hoặc</span>
                <div className="h-px flex-1 bg-surface-container"/>
            </div>

            {googleClientId ? (
                <div className="min-h-[44px]">
                    <div
                        ref={googleButtonRef}
                        aria-label="Đăng nhập bằng Google"
                        className={isGoogleReady ? 'flex justify-center' : 'hidden'}
                    />
                    {!isGoogleReady && (
                        <button
                            type="button"
                            disabled
                            className="flex w-full items-center justify-center gap-2 rounded-2xl border border-surface-container bg-surface-container px-4 py-3 text-xs font-black uppercase text-outline opacity-70"
                        >
                            <Loader2 className="h-4 w-4 animate-spin"/>
                            Đang tải Google
                        </button>
                    )}
                </div>
            ) : (
                <button
                    type="button"
                    onClick={handleMissingGoogleClientId}
                    disabled={isLoading}
                    className="flex w-full items-center justify-center gap-2 rounded-2xl border border-surface-container bg-surface-container-lowest px-4 py-3 text-xs font-black uppercase text-on-surface shadow-sm transition hover:border-primary/40 hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-60"
                >
                    <svg className="h-5 w-5" viewBox="-3 0 262 262" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid" fill="#000000"><g id="SVGRepo_bgCarrier" stroke-width="0"></g><g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"></g><g id="SVGRepo_iconCarrier"><path d="M255.878 133.451c0-10.734-.871-18.567-2.756-26.69H130.55v48.448h71.947c-1.45 12.04-9.283 30.172-26.69 42.356l-.244 1.622 38.755 30.023 2.685.268c24.659-22.774 38.875-56.282 38.875-96.027" fill="#4285F4"></path><path d="M130.55 261.1c35.248 0 64.839-11.605 86.453-31.622l-41.196-31.913c-11.024 7.688-25.82 13.055-45.257 13.055-34.523 0-63.824-22.773-74.269-54.25l-1.531.13-40.298 31.187-.527 1.465C35.393 231.798 79.49 261.1 130.55 261.1" fill="#34A853"></path><path d="M56.281 156.37c-2.756-8.123-4.351-16.827-4.351-25.82 0-8.994 1.595-17.697 4.206-25.82l-.073-1.73L15.26 71.312l-1.335.635C5.077 89.644 0 109.517 0 130.55s5.077 40.905 13.925 58.602l42.356-32.782" fill="#FBBC05"></path><path d="M130.55 50.479c24.514 0 41.05 10.589 50.479 19.438l36.844-35.974C195.245 12.91 165.798 0 130.55 0 79.49 0 35.393 29.301 13.925 71.947l42.211 32.783c10.59-31.477 39.891-54.251 74.414-54.251" fill="#EB4335"></path></g></svg>
                    Tiếp tục với Google
                </button>
            )}
        </div>
    );

    return (
        <div className="min-h-screen pt-24 pb-12 flex items-center justify-center bg-surface px-4">
            <div
                className="relative w-full max-w-md bg-surface-container-lowest rounded-3xl shadow-xl border border-surface-container/60 overflow-hidden">

                <div
                    className="relative h-28 bg-gradient-to-r from-primary to-primary-container flex items-center justify-center overflow-hidden">
                    <div
                        className="absolute inset-0 opacity-10 bg-[radial-gradient(#fff_1px,transparent_1px)] [background-size:16px_16px]"></div>
                    <div className="z-10 text-center">
                        <span
                            className="text-white text-3xl font-black tracking-widest bg-white/10 backdrop-blur-md px-6 py-1.5 rounded-2xl border border-white/20">
                            SOBU
                        </span>
                    </div>
                </div>

                <div className="flex border-b border-surface-container/60">
                    <button
                        onClick={() => setActiveTab('login')}
                        className={`flex-1 py-4 text-sm font-black uppercase tracking-wider transition-all relative ${
                            activeTab === 'login' ? 'text-primary' : 'text-outline/70 hover:text-on-surface'
                        }`}
                    >
                        Đăng nhập
                        {activeTab === 'login' &&
                            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"/>}
                    </button>
                    <button
                        onClick={() => setActiveTab('register')}
                        className={`flex-1 py-4 text-sm font-black uppercase tracking-wider transition-all relative ${
                            activeTab === 'register' ? 'text-primary' : 'text-outline/70 hover:text-on-surface'
                        }`}
                    >
                        Đăng ký
                        {activeTab === 'register' &&
                            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"/>}
                    </button>
                </div>

                <div className="p-6">
                    {(error || localError) && (
                        <div
                            className="mb-4 p-3.5 rounded-xl bg-error/10 border border-error/20 text-error text-xs font-bold flex gap-2.5 items-start">
                            <span className="shrink-0 w-1.5 h-1.5 rounded-full bg-error mt-1.5"/>
                            <span>{localError || error}</span>
                        </div>
                    )}

                    {activeTab === 'login' ? (
                        <>
                            <form onSubmit={handleLoginSubmit} className="space-y-4">
                                <div>
                                    <label
                                        className="block text-xs font-bold text-outline uppercase mb-1.5 pl-1">Email</label>
                                    <div className="relative">
                                        <Mail className="absolute left-3.5 top-3.5 w-4 h-4 text-outline/60"/>
                                        <input type="email" value={loginEmail}
                                               onChange={(e) => {
                                                   setLoginEmail(e.target.value);
                                                   clearError();
                                                   setLocalError(null);
                                               }} disabled={isLoading}
                                               className="w-full bg-surface-container rounded-2xl pl-11 pr-4 py-3.5 text-xs font-semibold focus:ring-2 outline-none text-on-surface"
                                               required/>
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1.5 pl-1">Mật khẩu</label>
                                    <div className="relative">
                                        <Lock className="absolute left-3.5 top-3.5 w-4 h-4 text-outline/60"/>
                                        <input type={showPassword ? 'text' : 'password'} value={loginPassword}
                                               onChange={(e) => {
                                                   setLoginPassword(e.target.value);
                                                   clearError();
                                                   setLocalError(null);
                                               }} disabled={isLoading}
                                               className="w-full bg-surface-container rounded-2xl pl-11 pr-11 py-3.5 text-xs font-semibold focus:ring-2 outline-none text-on-surface"
                                               required/>
                                        <button type="button" onClick={() => setShowPassword(!showPassword)}
                                                className="absolute right-3.5 top-3.5 text-outline/60">
                                            {showPassword ? <EyeOff className="w-4 h-4"/> : <Eye className="w-4 h-4"/>}
                                        </button>
                                    </div>
                                </div>
                                <button type="submit" disabled={isLoading}
                                        className="w-full py-4 mt-2 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase text-center shadow-lg flex justify-center gap-2">
                                    {isLoading ? <Loader2 className="w-4 h-4 animate-spin"/> : <span>Đăng nhập</span>}
                                </button>
                            </form>
                            {renderGoogleLogin()}
                        </>
                    ) : (
                        <>
                            <form onSubmit={handleRegisterSubmit} className="space-y-4">
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1.5 pl-1">Họ tên</label>
                                    <div className="relative">
                                        <User className="absolute left-3.5 top-3.5 w-4 h-4 text-outline/60"/>
                                        <input type="text" value={regName} onChange={(e) => setRegName(e.target.value)}
                                               disabled={isLoading}
                                               className="w-full bg-surface-container rounded-2xl pl-11 pr-4 py-3.5 text-xs font-semibold outline-none text-on-surface"
                                               required/>
                                    </div>
                                </div>
                                <div>
                                    <label
                                        className="block text-xs font-bold text-outline uppercase mb-1.5 pl-1">Email</label>
                                    <div className="relative">
                                        <Mail className="absolute left-3.5 top-3.5 w-4 h-4 text-outline/60"/>
                                        <input type="email" value={regEmail} onChange={(e) => setRegEmail(e.target.value)}
                                               disabled={isLoading}
                                               className="w-full bg-surface-container rounded-2xl pl-11 pr-4 py-3.5 text-xs font-semibold outline-none text-on-surface"
                                               required/>
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1.5 pl-1">Số điện thoại</label>
                                    <div className="relative">
                                        <Phone className="absolute left-3.5 top-3.5 w-4 h-4 text-outline/60"/>
                                        <input type="tel" value={regPhone} onChange={(e) => setRegPhone(e.target.value)}
                                               disabled={isLoading}
                                               className="w-full bg-surface-container rounded-2xl pl-11 pr-4 py-3.5 text-xs font-semibold outline-none text-on-surface"
                                               required/>
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1.5 pl-1">Mật khẩu</label>
                                    <div className="relative">
                                        <Lock className="absolute left-3.5 top-3.5 w-4 h-4 text-outline/60"/>
                                        <input type={showPassword ? 'text' : 'password'} value={regPassword}
                                               onChange={(e) => setRegPassword(e.target.value)} disabled={isLoading}
                                               className="w-full bg-surface-container rounded-2xl pl-11 pr-11 py-3.5 text-xs font-semibold outline-none text-on-surface"
                                               required/>
                                        <button type="button" onClick={() => setShowPassword(!showPassword)}
                                                className="absolute right-3.5 top-3.5 text-outline/60">
                                            {showPassword ? <EyeOff className="w-4 h-4"/> : <Eye className="w-4 h-4"/>}
                                        </button>
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-outline uppercase mb-1.5 pl-1">Nhập lại mật khẩu</label>
                                    <div className="relative">
                                        <Lock className="absolute left-3.5 top-3.5 w-4 h-4 text-outline/60"/>
                                        <input type={showConfirmPassword ? 'text' : 'password'} value={regConfirmPassword}
                                               onChange={(e) => setRegConfirmPassword(e.target.value)} disabled={isLoading}
                                               className="w-full bg-surface-container rounded-2xl pl-11 pr-11 py-3.5 text-xs font-semibold outline-none text-on-surface"
                                               required/>
                                        <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                                className="absolute right-3.5 top-3.5 text-outline/60">
                                            {showConfirmPassword ? <EyeOff className="w-4 h-4"/> : <Eye className="w-4 h-4"/>}
                                        </button>
                                    </div>
                                </div>
                                <button type="submit" disabled={isLoading}
                                        className="w-full py-4 mt-2 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase text-center shadow-lg flex justify-center gap-2">
                                    {isLoading ? <Loader2 className="w-4 h-4 animate-spin"/> : <span>Đăng ký</span>}
                                </button>
                            </form>
                            {renderGoogleLogin()}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}
