import Footer from "../components/common/Footer";
import Header from "../components/common/Header";
import { PrimaryButton, TertiaryButton } from "../components/ui/Button"
import { ProductCard } from "../components/ui/ProductCard";
import logo from "../assets/logo.png";

const Homepage = () => {
    return (
        <>
            <Header />
            <div>content...
                <PrimaryButton children={"Xem thêm"} />
                <TertiaryButton children={"Xem thêm"} />
                <ProductCard image={logo} title={"Ô tô CX5"} price={100000} categories={["ô tô", "lắp ráp"]} tag={"new"} />
            </div>
            <Footer />
        </>
    )
}

export default Homepage;