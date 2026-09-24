
import React, { useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import {
  CardNumberElement,
  CardExpiryElement,
  CardCvcElement,
  useStripe,
  useElements,
} from "@stripe/react-stripe-js";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";

import { selectUser } from "../store/auth-slice";
import {
  selectCartItems,
  selectTotalPrice,
  clearCart,
} from "../store/cart-slice";
import apiClient from "../api/apiClient";
import PageTitle from "./PageTitle";

export default function CheckoutForm() {
  const user = useSelector(selectUser);
  const cart = useSelector(selectCartItems);
  const totalPrice = useSelector(selectTotalPrice);

  const dispatch = useDispatch();
  const stripe = useStripe();
  const elements = useElements();
  const navigate = useNavigate();

  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const [cardComplete, setCardComplete] = useState({
    cardNumber: false,
    cardExpiry: false,
    cardCvc: false,
  });

  const [elementErrors, setElementErrors] = useState({
    cardNumber: "",
    cardExpiry: "",
    cardCvc: "",
  });

  const [cardNumberReady, setCardNumberReady] = useState(false);

  const isDarkMode = localStorage.getItem("theme") === "dark";

  const labelStyle =
    "block text-lg font-semibold text-primary dark:text-light mb-2";

  const fieldBaseClass =
    "w-full px-4 py-3 text-base border rounded-md transition " +
    "border-primary dark:border-light focus:ring focus:ring-dark " +
    "dark:focus:ring-lighter focus:outline-none text-gray-800 " +
    "dark:text-lighter bg-white dark:bg-gray-600";

  const getClassForElement = (field) =>
    `${fieldBaseClass} ${
      elementErrors[field]
        ? "border-red-500 focus:ring-red-500"
        : ""
    }`;

  const elementOptions = {
    style: {
      base: {
        fontSize: "16px",
        color: isDarkMode ? "#E5E7EB" : "#374151",
        "::placeholder": {
          color: isDarkMode ? "#D1D5DB" : "#9CA3AF",
        },
      },
      invalid: {
        color: "#F87171",
      },
    },
  };

  function handleCardChange(field, event) {
    setElementErrors((prev) => ({
      ...prev,
      [field]: event.error?.message || "",
    }));

    setCardComplete((prev) => ({
      ...prev,
      [field]: event.complete,
    }));
  }

  // Stripe accepts a 2-letter ISO country code.
  // Omit country if the user's profile does not contain a valid value.
  function getBillingDetails() {
    const address = {};

    const line1 = user?.street?.trim();
    const city = user?.city?.trim();
    const state = user?.state?.trim();
    const postalCode = user?.postalCode?.trim();

    if (line1) address.line1 = line1;
    if (city) address.city = city;
    if (state) address.state = state;
    if (postalCode) address.postal_code = postalCode;

    const country = user?.country?.trim()?.toUpperCase();

    if (country && /^[A-Z]{2}$/.test(country)) {
      address.country = country;
    }

    const billingDetails = {};

    const name = user?.name?.trim();
    const email = user?.email?.trim();
    const phone = user?.mobileNumber?.trim();

    if (name) billingDetails.name = name;
    if (email) billingDetails.email = email;
    if (phone) billingDetails.phone = phone;

    if (Object.keys(address).length > 0) {
      billingDetails.address = address;
    }

    return billingDetails;
  }

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (isProcessing) return;

    setErrorMessage("");

    if (!stripe || !elements) {
      setErrorMessage("Stripe is still loading. Please try again.");
      return;
    }

    if (!cardNumberReady) {
      setErrorMessage("Card field is loading. Please wait.");
      return;
    }

    if (!Object.values(cardComplete).every(Boolean)) {
      setErrorMessage("Please enter complete card details.");
      return;
    }

    if (Object.values(elementErrors).some(Boolean)) {
      setErrorMessage("Please correct the card details.");
      return;
    }

    if (!cart || cart.length === 0) {
      setErrorMessage("Your cart is empty.");
      return;
    }

    const amountInCents = Math.round(Number(totalPrice) * 100);

    if (!Number.isFinite(amountInCents) || amountInCents <= 0) {
      setErrorMessage("Invalid payment amount.");
      return;
    }

    const cardNumberElement =
      elements.getElement(CardNumberElement);

    if (!cardNumberElement) {
      setErrorMessage(
        "Card field is not ready. Please refresh and try again."
      );
      return;
    }

    setIsProcessing(true);

    try {
      // Step 1: Create PaymentIntent on backend.
      // VITE_API_BASE_URL already contains /api/v1.
      const response = await apiClient.post(
        "/payment/create-payment-intent",
        {
          amount: amountInCents,
          currency: "usd",
        }
      );

      const clientSecret = response?.data?.clientSecret;

      if (!clientSecret) {
        throw new Error(
          "Payment client secret was not returned by the server."
        );
      }

      // Step 2: Confirm payment with Stripe.
      const result = await stripe.confirmCardPayment(
        clientSecret,
        {
          payment_method: {
            card: cardNumberElement,
            billing_details: getBillingDetails(),
          },
        }
      );

      if (result.error) {
        setErrorMessage(
          result.error.message || "Payment failed. Please try again."
        );
        return;
      }

      const paymentIntent = result.paymentIntent;

      if (paymentIntent?.status !== "succeeded") {
        setErrorMessage(
          `Payment status: ${paymentIntent?.status || "unknown"}.`
        );
        return;
      }

      // Step 3: Create order after successful payment.
      try {
        await apiClient.post("/orders", {
          totalPrice: Number(totalPrice),
          paymentId: paymentIntent.id,
          paymentStatus: paymentIntent.status,
          items: cart.map((item) => ({
            productId: item.productId,
            quantity: item.quantity,
            price: item.price,
          })),
        });

        toast.success("Payment successful!");

        sessionStorage.setItem("skipRedirectPath", "true");
        dispatch(clearCart());
        navigate("/order-success");
      } catch (orderError) {
        console.error("Order creation failed:", orderError);

        setErrorMessage(
          "Payment succeeded, but order creation failed. " +
            "Please contact support with payment ID: " +
            paymentIntent.id
        );
      }
    } catch (error) {
      console.error("Error processing payment:", error);

      setErrorMessage(
        error.response?.data?.message ||
          error.message ||
          "Error processing payment. Please try again later."
      );
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="min-h-[852px] flex items-center justify-center font-primary dark:bg-darkbg">
      <div className="bg-white dark:bg-gray-700 shadow-md rounded-lg max-w-md w-full px-8 py-6">
        <PageTitle title="Complete Your Payment" />

        <p className="text-center mt-8 text-lg text-gray-600 dark:text-lighter mb-8">
          Amount to be charged:{" "}
          <strong>${Number(totalPrice).toFixed(2)}</strong>
        </p>

        {errorMessage && (
          <div
            role="alert"
            className="text-red-500 text-sm text-center mb-4"
          >
            {errorMessage}
          </div>
        )}

        {isProcessing && (
          <p className="text-center text-sm mb-4 text-primary dark:text-light">
            Processing payment... Please don't refresh the page.
          </p>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className={labelStyle}>Card Number</label>

            <div className={getClassForElement("cardNumber")}>
              <CardNumberElement
                options={elementOptions}
                onReady={() => setCardNumberReady(true)}
                onChange={(event) =>
                  handleCardChange("cardNumber", event)
                }
              />
            </div>

            {elementErrors.cardNumber && (
              <p className="text-red-500 text-sm mt-1">
                {elementErrors.cardNumber}
              </p>
            )}
          </div>

          <div>
            <label className={labelStyle}>Expiry Date</label>

            <div className={getClassForElement("cardExpiry")}>
              <CardExpiryElement
                options={elementOptions}
                onChange={(event) =>
                  handleCardChange("cardExpiry", event)
                }
              />
            </div>

            {elementErrors.cardExpiry && (
              <p className="text-red-500 text-sm mt-1">
                {elementErrors.cardExpiry}
              </p>
            )}
          </div>

          <div>
            <label className={labelStyle}>CVC</label>

            <div className={getClassForElement("cardCvc")}>
              <CardCvcElement
                options={elementOptions}
                onChange={(event) =>
                  handleCardChange("cardCvc", event)
                }
              />
            </div>

            {elementErrors.cardCvc && (
              <p className="text-red-500 text-sm mt-1">
                {elementErrors.cardCvc}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={
              !stripe ||
              !elements ||
              !cardNumberReady ||
              isProcessing
            }
            className="w-full px-6 py-3 mt-6 text-white dark:text-black text-xl bg-primary dark:bg-light hover:bg-dark dark:hover:bg-lighter rounded-md transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isProcessing ? "Processing Payment..." : "Pay Now"}
          </button>
        </form>
      </div>
    </div>
  );
}