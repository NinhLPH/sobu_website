import React, {useState, useEffect} from 'react';
import {Mail, Lock, User, Phone, Eye, EyeOff, Loader2, CheckCircle2} from 'lucide-react';
import {useAuthStore} from '../store/useAuthStore';
import {useNavigate} from 'react-router-dom';

export default function LoginPage() {
    const navigate = useNavigate();
    const {
        loginAction,
        registerAction,
        isLoading,
        error,
        clearError,
        isAuthenticated
    } = useAuthStore();

    const [activeTab, setActiveTab] = useState<'login' | 'register'>('login');
    const [showPassword, setShowPassword] = useState(false);

    const [loginEmail, setLoginEmail] = useState('');
    const [loginPassword, setLoginPassword] = useState('');

    const [regName, setRegName] = useState('');
    const [regEmail, setRegEmail] = useState('');
    const [regPhone, setRegPhone] = useState('');
    const [regPassword, setRegPassword] = useState('');
    const [regConfirmPassword, setRegConfirmPassword] = useState('');
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const [registerSuccess, setRegisterSuccess] = useState(false);
    const [registerMessage, setRegisterMessage] = useState('');
    const [localError, setLocalError] = useState<string | null>(null);

    // Nếu đã đăng nhập thì đá về trang chủ
    useEffect(() => {
        if (isAuthenticated) {
            navigate('/');
        }
    }, [isAuthenticated, navigate]);

    useEffect(() => {
        clearError();
        setLocalError(null);
    }, [activeTab, clearError]);

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
        setRegisterSuccess(false);

        if (!regName || !regEmail || !regPhone || !regPassword || !regConfirmPassword) {
            setLocalError('Vui lòng điền đầy đủ các thông tin đăng ký!');
            return;
        }

        if (regPassword !== regConfirmPassword) {
            setLocalError('Mật khẩu nhập lại không khớp!');
            return;
        }

        try {
            const account = await registerAction({
                email: regEmail.trim(),
                password: regPassword,
                fullName: regName.trim(),
                phone: regPhone.trim()
            });

            setRegisterMessage(account.message);
            setRegisterSuccess(true);
        } catch {
            // The store exposes the backend message through `error`.
        }
    };

    return (
        <div className="min-h-screen pt-24 pb-12 flex items-center justify-center bg-surface px-4">
            <div
                className="relative w-full max-w-md bg-surface-container-lowest rounded-3xl shadow-xl border border-surface-container/60 overflow-hidden">

                {/* Banner Graphic */}
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

                {/* Tabs selection */}
                {!registerSuccess && (
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
                )}

                {/* Content form container */}
                <div className="p-6">
                    {(error || localError) && (
                        <div
                            className="mb-4 p-3.5 rounded-xl bg-error/10 border border-error/20 text-error text-xs font-bold flex gap-2.5 items-start">
                            <span className="shrink-0 w-1.5 h-1.5 rounded-full bg-error mt-1.5"/>
                            <span>{localError || error}</span>
                        </div>
                    )}

                    {registerSuccess ? (
                        <div className="py-8 flex flex-col items-center text-center">
                            <div className="w-16 h-16 rounded-full bg-primary-container/20 flex items-center justify-center text-primary mb-4">
                                <CheckCircle2 className="w-10 h-10 stroke-[2]"/>
                            </div>
                            <h3 className="text-lg font-black text-on-surface mb-2">Đăng ký thành công!</h3>
                            <p className="text-sm text-outline mb-4 font-medium">
                                {registerMessage || 'Vui lòng kiểm tra email của bạn để kích hoạt tài khoản trước khi đăng nhập.'}
                            </p>
                            <button
                                onClick={() => {
                                    setRegisterSuccess(false);
                                    setActiveTab('login');
                                }}
                                className="px-6 py-2.5 bg-primary text-white rounded-xl text-xs font-black uppercase shadow-md"
                            >
                                Đăng nhập ngay
                            </button>
                            <button
                                onClick={() => navigate(`/verify-email?email=${encodeURIComponent(regEmail)}`)}
                                className="mt-3 px-6 py-2.5 bg-surface-container text-on-surface rounded-xl text-xs font-black uppercase"
                            >
                                Gửi lại email kích hoạt
                            </button>
                        </div>
                    ) : activeTab === 'login' ? (
                        <form onSubmit={handleLoginSubmit} className="space-y-4">
                            {/* LOGIN INPUTS */}
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
                                <label className="block text-xs font-bold text-outline uppercase mb-1.5 pl-1">Mật
                                    khẩu</label>
                                <div className="relative">
                                    <Lock className="absolute left-3.5 top-3.5 w-4 h-4 text-outline/60"/>
                                    <input type={showPassword ? "text" : "password"} value={loginPassword}
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
                    ) : (
                        <form onSubmit={handleRegisterSubmit} className="space-y-4">
                            {/* REGISTER INPUTS */}
                            <div>
                                <label className="block text-xs font-bold text-outline uppercase mb-1.5 pl-1">Họ
                                    tên</label>
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
                                <label className="block text-xs font-bold text-outline uppercase mb-1.5 pl-1">Số điện
                                    thoại</label>
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
                                    <input type={showPassword ? "text" : "password"} value={regPassword}
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
                                    <input type={showConfirmPassword ? "text" : "password"} value={regConfirmPassword}
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
                    )}
                </div>
            </div>
        </div>
    );
}
