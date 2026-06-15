import {useParams, Link} from 'react-router-dom';
import {ChevronLeft} from 'lucide-react';
import {mockBlogs} from '../data/mockData';

export default function BlogDetail() {
    const {id} = useParams();
    const blog = mockBlogs.find(b => b.id === id) || mockBlogs[0];

    return (
        <main className="pt-32 pb-24 bg-surface px-6">
            <div className="max-w-3xl mx-auto">
                <Link to="/blog"
                      className="inline-flex items-center gap-2 text-sm font-bold text-outline hover:text-primary mb-8 transition-colors">
                    <ChevronLeft className="w-4 h-4"/> Quay lại Blog
                </Link>

                <h1 className="text-4xl md:text-5xl font-black text-on-surface mb-6 leading-tight">{blog.title}</h1>
                <div className="text-sm font-bold text-outline uppercase tracking-widest mb-10">{blog.date}</div>

                <div className="aspect-[21/9] rounded-[2rem] overflow-hidden mb-12 shadow-md">
                    <img src={blog.image} className="w-full h-full object-cover" alt={blog.title}/>
                </div>

                <div
                    className="prose prose-lg text-on-surface-variant font-medium prose-headings:font-black prose-headings:text-on-surface">
                    <p className="text-xl leading-relaxed text-on-surface mb-6">{blog.excerpt}</p>
                    <p className="leading-relaxed">{blog.content}</p>
                    <p className="leading-relaxed">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam in
                        dui mauris. Vivamus hendrerit arcu sed erat molestie vehicula. Sed auctor neque eu tellus
                        rhoncus ut eleifend nibh porttitor.</p>
                </div>
            </div>
        </main>
    );
}