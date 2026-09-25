import React, { useEffect } from "react";
import PageTitle from "./PageTitle";
import {
  Link,
  Form,
  useActionData,
  useNavigation,
  useNavigate,
} from "react-router-dom";
import apiClient from "../api/apiClient";
import { toast } from "react-toastify";
import { useDispatch } from "react-redux";
import { loginSuccess } from "../store/auth-slice";

export default function Login() {
  const actionData = useActionData();
  const navigation = useNavigation();
  const isSubmitting = navigation.state === "submitting";
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const from = sessionStorage.getItem("redirectPath") || "/home";

  useEffect(() => {
    // =========================
    // Google OAuth Login
    // =========================
    const params = new URLSearchParams(window.location.search);

    const oauth2 = params.get("oauth2");
    const token = params.get("token");
    const name = params.get("name");
    const email = params.get("email");

    if (oauth2 === "success" && token) {
      const user = {
        name: name || "",
        email: email || "",
      };

      dispatch(
        loginSuccess({
          jwtToken: token,
          user: user,
        })
      );

      sessionStorage.removeItem("redirectPath");

      // Remove token from URL after reading it
      window.history.replaceState(
        {},
        document.title,
        "/login"
      );

      navigate(from, { replace: true });

      return;
    }

    // =========================
    // Google OAuth Failed
    // =========================
    if (oauth2 === "error") {
      toast.error("Google login failed.");
      return;
    }

    // =========================
    // Normal Email/Password Login
    // =========================
    if (actionData?.success) {
      dispatch(
        loginSuccess({
          jwtToken: actionData.jwtToken,
          user: actionData.user,
        })
      );

      sessionStorage.removeItem("redirectPath");

      setTimeout(() => {
        navigate(from);
      }, 100);
    } else if (actionData?.errors) {
      toast.error(
        actionData.errors.message || "Login failed."
      );
    }
  }, [actionData, dispatch, navigate, from]);

  const labelStyle =
    "block text-lg font-semibold text-primary dark:text-light mb-2";

  const textFieldStyle =
    "w-full px-4 py-2 text-base border rounded-md transition border-primary dark:border-light focus:ring focus:ring-dark dark:focus:ring-lighter focus:outline-none text-gray-800 dark:text-lighter bg-white dark:bg-gray-600 placeholder-gray-400 dark:placeholder-gray-300";

  return (
    <div className="min-h-[852px] flex items-center justify-center font-primary dark:bg-darkbg">
      <div className="bg-white dark:bg-gray-700 shadow-md rounded-lg max-w-md w-full px-8 py-6">

        {/* Title */}
        <PageTitle title="Login" />

        {/* Login Form */}
        <Form method="POST" className="space-y-6">

          {/* Username */}
          <div>
            <label
              htmlFor="username"
              className={labelStyle}
            >
              Username
            </label>

            <input
              id="username"
              type="text"
              name="username"
              placeholder="Enter email"
              autoComplete="username"
              required
              className={textFieldStyle}
            />
          </div>

          {/* Password */}
          <div>
            <label
              htmlFor="password"
              className={labelStyle}
            >
              Password
            </label>

            <input
              id="password"
              type="password"
              name="password"
              placeholder="Your Password"
              autoComplete="current-password"
              required
              minLength={4}
              maxLength={20}
              className={textFieldStyle}
            />
          </div>

          {/* Login Button */}
          <div>
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full px-6 py-2 text-white dark:text-black text-xl rounded-md transition duration-200 bg-primary dark:bg-light hover:bg-dark dark:hover:bg-lighter"
            >
              {isSubmitting
                ? "Authenticating..."
                : "Login"}
            </button>
          </div>
        </Form>

        {/* Google Login */}
        <div className="mt-4">
          <button
            type="button"
            onClick={() => {
              window.location.href =
                "https://eazystore-backend-vr41.onrender.com/oauth2/authorization/google?prompt=select_account";
            }}
            className="w-full px-6 py-2 border border-gray-300 rounded-md
              text-gray-700 dark:text-white font-semibold
              hover:bg-gray-100 dark:hover:bg-gray-600 transition"
          >
            Continue with Google
          </button>
        </div>

        {/* Register Link */}
        <p className="text-center text-gray-600 dark:text-gray-400 mt-4">
          Don't have an account?{" "}
          <Link
            to="/register"
            className="text-primary dark:text-light hover:text-dark dark:hover:text-primary transition duration-200"
          >
            Register Here
          </Link>
        </p>

      </div>
    </div>
  );
}

export async function loginAction({ request }) {
  const data = await request.formData();

  const loginData = {
    username: data.get("username"),
    password: data.get("password"),
  };

  try {
    const response = await apiClient.post(
      "/auth/login",
      loginData
    );

    const {
      message,
      user,
      jwtToken,
    } = response.data;

    return {
      success: true,
      message,
      user,
      jwtToken,
    };

  } catch (error) {

    if (error.response?.status === 401) {
      return {
        success: false,
        errors: {
          message: "Invalid username or password",
        },
      };
    }

    throw new Response(
      error.response?.data?.message ||
        error.message ||
        "Failed to login. Please try again.",
      {
        status: error.response?.status || 500,
      }
    );
  }
}