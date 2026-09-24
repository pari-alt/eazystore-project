import Header from "./components/Header";
import Footer from "./components/footer/Footer";
import React from "react";
import { Outlet, useNavigation } from "react-router-dom";
import AIShoppingAssistant from "./components/AIShoppingAssistant";

function App() {
  const navigation = useNavigation();

  return (
    <>
      <Header />

      {navigation.state === "loading" ? (
        <div className="flex items-center justify-center min-h-[852px]">
          <span className="text-4xl font-semibold text-primary dark:text-light">
            Loading...
          </span>
        </div>
      ) : (
        <Outlet />
      )}

      <Footer />

      <AIShoppingAssistant />
    </>
  );
}

export default App;