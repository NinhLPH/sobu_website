import {Clock, ExternalLink, Mail, MapPin, Phone} from 'lucide-react';
import {Link} from 'react-router-dom';
import {usePublicUiStore} from '../../store/usePublicUiStore';
import {parseJsonConfig} from '../../utils/website-config';

interface FooterLink {
    label: string;
    href: string;
}

type SocialLinks = Record<string, string>;

const isExternalUrl = (href: string) => /^https?:\/\//i.test(href);

function FooterNavLink({link}: {link: FooterLink}) {
    if (!link.href || !link.label) return null;

    if (isExternalUrl(link.href)) {
        return (
            <a
                href={link.href}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 transition-colors hover:text-primary"
            >
                {link.label}
                <ExternalLink className="h-3 w-3"/>
            </a>
        );
    }

    return (
        <Link to={link.href} className="transition-colors hover:text-primary">
            {link.label}
        </Link>
    );
}

export default function Footer() {
    const configMap = usePublicUiStore((state) => state.configMap);
    const siteName = configMap?.site_name || 'SOBU';
    const greetingText = configMap?.footer_greeting_text;
    const supportHotline = configMap?.support_hotline;
    const supportEmail = configMap?.support_email;
    const companyAddress = configMap?.company_address;
    const workingHours = configMap?.working_hours;
    const copyrightText = configMap?.copyright_text;
    const newsletterEnabled = configMap?.newsletter_enabled === 'true';
    const newsletterDescription = configMap?.newsletter_description;
    const newsletterSubmitLabel = configMap?.newsletter_submit_label;
    const companyLinks = parseJsonConfig<FooterLink[]>(configMap, 'footer_company_links', []);
    const helpLinks = parseJsonConfig<FooterLink[]>(configMap, 'footer_help_links', []);
    const legalLinks = parseJsonConfig<FooterLink[]>(configMap, 'legal_links', []);
    const socialLinks = parseJsonConfig<SocialLinks>(configMap, 'social_links', {});
    const visibleSocialLinks = Object.entries(socialLinks).filter(([, href]) => Boolean(href));

    return (
        <footer className="mt-auto w-full border-t border-surface-container-high/60 bg-surface-container-high pb-6 pt-16 text-on-surface">
            <div className="mx-auto max-w-[1504px] px-4 sm:px-6 lg:px-8">
                <div className="mb-12 grid grid-cols-1 gap-10 md:grid-cols-12">
                    <div className="col-span-1 flex flex-col justify-start pr-0 md:col-span-4 md:pr-10">
                        <Link
                            to="/"
                            className="mb-4 inline-block w-fit select-none text-3xl font-black tracking-widest text-on-surface transition-colors hover:text-primary lg:text-4xl"
                        >
                            {siteName}
                        </Link>

                        {greetingText && (
                            <p className="mb-6 max-w-sm text-xs font-semibold leading-relaxed text-on-surface-variant">
                                {greetingText}
                            </p>
                        )}

                        {newsletterEnabled && newsletterDescription && newsletterSubmitLabel && (
                            <div className="flex max-w-sm flex-col gap-2.5">
                                <p className="text-xs font-semibold leading-relaxed text-on-surface-variant">
                                    {newsletterDescription}
                                </p>
                                <div className="relative w-full">
                                    <Mail className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-outline"/>
                                    <input
                                        type="email"
                                        placeholder="Email"
                                        className="w-full rounded-xl border border-surface-container bg-surface-container-lowest py-2.5 pl-11 pr-4 text-xs font-medium text-on-surface outline-none transition-all focus:ring-2 focus:ring-primary/20"
                                    />
                                </div>
                                <button
                                    className="w-full cursor-pointer rounded-xl bg-primary py-2.5 text-xs font-black uppercase tracking-widest text-on-primary shadow-md shadow-primary/10 transition-colors hover:bg-primary-container"
                                >
                                    {newsletterSubmitLabel}
                                </button>
                            </div>
                        )}
                    </div>

                    <div className="col-span-1 grid grid-cols-1 gap-8 pt-2 sm:grid-cols-3 md:col-span-8">
                        {companyLinks.length > 0 && (
                            <div>
                                <h4 className="mb-5 text-[11px] font-black uppercase tracking-widest text-outline">Company</h4>
                                <ul className="space-y-3.5 text-xs font-bold text-on-surface-variant">
                                    {companyLinks.map((link) => (
                                        <li key={`${link.label}-${link.href}`}>
                                            <FooterNavLink link={link}/>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}

                        {helpLinks.length > 0 && (
                            <div>
                                <h4 className="mb-5 text-[11px] font-black uppercase tracking-widest text-outline">Help Center</h4>
                                <ul className="space-y-3.5 text-xs font-bold text-on-surface-variant">
                                    {helpLinks.map((link) => (
                                        <li key={`${link.label}-${link.href}`}>
                                            <FooterNavLink link={link}/>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}

                        <div>
                            <h4 className="mb-5 text-[11px] font-black uppercase tracking-widest text-outline">Contact Info</h4>
                            <ul className="mb-5 space-y-3.5 text-xs font-bold text-on-surface-variant">
                                {supportHotline && (
                                    <li className="flex items-start gap-2.5">
                                        <Phone className="mt-0.5 h-4 w-4 shrink-0 text-primary"/>
                                        <a href={`tel:${supportHotline.replace(/\s+/g, '')}`} className="hover:text-primary">
                                            {supportHotline}
                                        </a>
                                    </li>
                                )}
                                {supportEmail && (
                                    <li className="flex items-start gap-2.5">
                                        <Mail className="mt-0.5 h-4 w-4 shrink-0 text-primary"/>
                                        <a href={`mailto:${supportEmail}`} className="break-all hover:text-primary">
                                            {supportEmail}
                                        </a>
                                    </li>
                                )}
                                {companyAddress && (
                                    <li className="flex items-start gap-2.5">
                                        <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-primary"/>
                                        <span>{companyAddress}</span>
                                    </li>
                                )}
                                {workingHours && (
                                    <li className="flex items-start gap-2.5">
                                        <Clock className="mt-0.5 h-4 w-4 shrink-0 text-primary"/>
                                        <span>{workingHours}</span>
                                    </li>
                                )}
                            </ul>

                            {visibleSocialLinks.length > 0 && (
                                <div className="flex flex-wrap gap-2.5">
                                    {visibleSocialLinks.map(([name, href]) => (
                                        <a
                                            key={name}
                                            href={href}
                                            target="_blank"
                                            rel="noreferrer"
                                            className="rounded-lg bg-surface-container-highest px-3 py-2 text-[10px] font-black uppercase text-on-surface transition-colors hover:text-primary"
                                        >
                                            {name}
                                        </a>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {(copyrightText || legalLinks.length > 0) && (
                    <div className="flex flex-col items-center justify-center gap-2 border-t border-surface-container-high pt-6 text-center text-[11px] font-bold text-outline">
                        {copyrightText && <p>{copyrightText}</p>}
                        {legalLinks.length > 0 && (
                            <div className="flex flex-wrap items-center justify-center gap-x-3 gap-y-1">
                                {legalLinks.map((link) => (
                                    <FooterNavLink key={`${link.label}-${link.href}`} link={link}/>
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </footer>
    );
}
