import {Link} from 'react-router-dom';
import {mockBlogs, placeholderImages} from '../data/mockData';

export default function BlogList() {
    return (
        <main className="w-full min-w-0 space-y-16 bg-surface pb-24 pt-24 sm:space-y-24">
            {/* HERO */}
            <section className="w-full px-4 sm:px-6">
                <div
                    className="w-full h-[300px] md:h-[400px] bg-surface-container-low rounded-[2rem] overflow-hidden relative">
                    <img src={placeholderImages.hero}
                         className="w-full h-full object-cover opacity-50 mix-blend-multiply" alt="Blog Hero"/>
                    <div className="absolute inset-0 flex items-center justify-center">
                        <h1 className="text-4xl font-black uppercase tracking-tighter text-on-surface sm:text-5xl md:text-6xl">SOBU
                            BLOG</h1>
                    </div>
                </div>
            </section>

            {/* MAIN BLOGS */}
            <section className="w-full px-4 sm:px-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    {mockBlogs.slice(0, 3).map(blog => (
                        <Link to={`/blog/${blog.id}`} key={blog.id} className="group flex flex-col">
                            <div
                                className="aspect-[4/3] rounded-[2rem] overflow-hidden bg-surface-container mb-6 shadow-sm group-hover:shadow-md transition-all">
                                <img src={blog.image}
                                     className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
                                     alt={blog.title}/>
                            </div>
                            <h2 className="text-2xl font-black text-on-surface group-hover:text-primary transition-colors mb-3 leading-tight">{blog.title}</h2>
                            <p className="text-on-surface-variant font-medium mb-4">{blog.excerpt}</p>
                            <span
                                className="text-xs font-bold text-outline uppercase tracking-widest">{blog.date}</span>
                        </Link>
                    ))}
                </div>
            </section>

            {/* PHOTO GALLERY */}
            <section className="w-full px-4 text-center sm:px-6">
                <h2 className="text-4xl font-black uppercase mb-12 text-on-surface">Photo Gallery</h2>
                <div className="columns-2 md:columns-3 gap-4 space-y-4">
                    {placeholderImages.gallery.map((img, idx) => (
                        <div key={idx}
                             className="break-inside-avoid rounded-2xl overflow-hidden bg-surface-container-low shadow-sm hover:shadow-md transition-shadow">
                            <img src={img} className="w-full h-auto object-cover" alt={`Gallery ${idx}`}/>
                        </div>
                    ))}
                </div>
                <button
                    className="mt-12 px-10 py-3 border-2 border-on-surface rounded-full font-bold uppercase tracking-widest text-sm hover:bg-on-surface hover:text-surface transition-colors">See
                    more
                </button>
            </section>

            {/* MORE BLOGS */}
            <section className="w-full px-4 sm:px-6">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                    {mockBlogs.map(blog => (
                        <Link
                            to={`/blog/${blog.id}`}
                            key={blog.id}
                            className="bg-surface-container rounded-2xl overflow-hidden shadow-sm hover:shadow-md transition-all group flex flex-col"
                        >
                            {/* Ảnh của blog */}
                            <div className="aspect-[16/10] w-full overflow-hidden bg-surface-container-low">
                                <img
                                    src={blog.image}
                                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                    alt={blog.title}
                                />
                            </div>

                            {/* Text của blog */}
                            <div className="p-5 flex flex-col flex-1 justify-between">
                                <div>
                                    <h3 className="font-bold text-base text-on-surface group-hover:text-primary mb-2 line-clamp-2 transition-colors">
                                        {blog.title}
                                    </h3>
                                    <p className="text-xs text-outline line-clamp-3 mb-4">
                                        {blog.excerpt || blog.content}
                                    </p>
                                </div>
                                <span className="text-[10px] font-bold text-outline/70 uppercase tracking-wider">
                        {blog.date}
                    </span>
                            </div>
                        </Link>
                    ))}
                </div>
                <div className="flex justify-end mt-6">
                    <button
                        className="px-8 py-3 bg-surface-variant text-on-surface rounded-full font-bold uppercase tracking-widest text-sm hover:bg-outline-variant transition-colors">MORE
                        →
                    </button>
                </div>
            </section>
        </main>
    );
}
