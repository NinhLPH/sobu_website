import Footer from "../components/common/Footer";
import Header from "../components/common/Header";
import { PrimaryButton, TertiaryButton } from "../components/ui/Button"
const Homepage = () => {
    return (
        <>
            <Header/>
            <div>content...
                <PrimaryButton children={"Xem thêm"}/>
                <TertiaryButton children={"Xem thêm"}/>
            </div>
            <Footer/>
        </>
    )
}

export default Homepage;