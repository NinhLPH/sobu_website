import {HatGlasses, Mail} from 'lucide-react';
import {Link} from 'react-router-dom';
import {getPublicConfigValue, usePublicUiStore} from '../../store/usePublicUiStore';

export default function Footer() {
    const configs = usePublicUiStore((state) => state.configs);
    const siteName = getPublicConfigValue(configs, 'site_name', 'SOBU');
    const supportHotline = getPublicConfigValue(configs, 'support_hotline', '1234567890');

    return (
        <footer
            className="bg-surface-container-high w-full pt-16 pb-6 mt-auto text-on-surface border-t border-surface-container-high/60">
            <div className="mx-auto max-w-[1504px] px-4 sm:px-6 lg:px-8">
                <div className="grid grid-cols-1 md:grid-cols-12 gap-12 mb-12">
                    <div className="col-span-1 md:col-span-4 pr-0 md:pr-12 flex flex-col justify-start">
                        <Link
                            to="/"
                            className="inline-block text-3xl lg:text-4xl font-black tracking-widest text-on-surface hover:text-primary transition-colors mb-4 w-fit select-none"
                        >
                            {siteName}
                        </Link>

                        <p className="text-xs font-semibold text-on-surface-variant mb-6 leading-relaxed max-w-sm">
                            All good things come by email (including premium model toys). Sign up now to join our
                            collector community.
                        </p>

                        <div className="flex flex-col gap-2.5 max-w-sm">
                            <div className="relative w-full">
                                <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-outline w-4 h-4"/>
                                <input
                                    type="email"
                                    placeholder="Enter your email"
                                    className="w-full pl-11 pr-4 py-2.5 bg-surface-container-lowest border border-surface-container rounded-xl outline-none text-xs text-on-surface focus:ring-2 focus:ring-primary/20 font-medium transition-all"
                                />
                            </div>
                            <button
                                className="w-full py-2.5 bg-gradient-to-br from-primary to-primary-container text-white rounded-xl text-xs font-black uppercase tracking-widest hover:scale-[1.01] transition-transform shadow-md shadow-primary/10">
                                Subscribe
                            </button>
                        </div>
                    </div>

                    <div className="col-span-1 md:col-span-8 grid grid-cols-1 sm:grid-cols-3 gap-8 pt-2">
                        {/* Company Links */}
                        <div>
                            <h4 className="font-black uppercase tracking-widest text-[11px] text-outline mb-5">Company</h4>
                            <ul className="space-y-3.5 text-xs font-bold text-on-surface-variant">
                                <li><Link to="#" className="hover:text-primary transition-colors">About SOBU</Link></li>
                                <li><Link to="#" className="hover:text-primary transition-colors">Contact us</Link></li>
                                <li><Link to="#" className="hover:text-primary transition-colors">Blog & News</Link>
                                </li>
                                <li><Link to="#" className="hover:text-primary transition-colors">Customer
                                    Reviews</Link></li>
                            </ul>
                        </div>

                        {/* Help Center Links */}
                        <div>
                            <h4 className="font-black uppercase tracking-widest text-[11px] text-outline mb-5">Help
                                Center</h4>
                            <ul className="space-y-3.5 text-xs font-bold text-on-surface-variant">
                                <li><Link to="#" className="hover:text-primary transition-colors">Getting started</Link>
                                </li>
                                <li><Link to="#" className="hover:text-primary transition-colors">Pre-order
                                    Policy</Link></li>
                                <li><Link to="#" className="hover:text-primary transition-colors">Custom / Trade-in
                                    Service</Link></li>
                                <li><Link to="#" className="hover:text-primary transition-colors">Report a bug</Link>
                                </li>
                                <li><Link to="#" className="hover:text-primary transition-colors">Zalo Chat
                                    support</Link></li>
                            </ul>
                        </div>

                        {/* Contact Info */}
                        <div>
                            <h4 className="font-black uppercase tracking-widest text-[11px] text-outline mb-5">Contact
                                Info</h4>
                            <ul className="space-y-3.5 text-xs font-bold text-on-surface-variant mb-5">
                                <li className="flex flex-col"><span
                                    className="text-[10px] uppercase font-black text-outline/60">Hotline:</span> {supportHotline}
                                </li>
                                <li className="flex flex-col"><span
                                    className="text-[10px] uppercase font-black text-outline/60">Email:</span> sobu.studio@email.com
                                </li>
                                <li className="flex flex-col"><span
                                    className="text-[10px] uppercase font-black text-outline/60">Workshop Location:</span> Ha
                                    Nam - Viet Nam
                                </li>
                            </ul>

                            {/* Mạng xã hội */}
                            <div className="flex gap-2.5">
                                {[1, 2, 3, 4].map((_, i) => (
                                    <Link key={i} to="#"
                                          className="w-8 h-8 rounded-lg bg-surface-container-highest flex items-center justify-center text-on-surface hover:text-primary hover:scale-105 transition-all">
                                        <HatGlasses className="w-4 h-4"/>
                                    </Link>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>

                <div
                    className="pt-6 border-t border-surface-container-high flex flex-col items-center justify-center text-[11px] font-bold text-outline text-center gap-1.5">
                    <p>
                        Copyright © 2026 SOBU Studio | All Rights Reserved | {' '}
                        <Link to="#" className="underline hover:text-primary">Terms and Conditions</Link> | {' '}
                        <Link to="#" className="underline hover:text-primary">Privacy Policy</Link>
                    </p>
                </div>
            </div>
        </footer>
    );
}
