"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  AlertCircle,
  ArrowRight,
  FolderKanban,
  Eye,
  EyeOff,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";

import { useAuth } from "@/lib/AuthContext";
import axiosInstance from "@/lib/Axiosinstance";

export default function Page() {
  const router = useRouter();
  const { login } = useAuth();

  const [isSignUp, setIsSignUp] = useState(false);
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
  });

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));

    setError("");
    setMessage("");
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    setIsLoading(true);
    setError("");
    setMessage("");

    try {
      if (isSignUp) {
        const res = await axiosInstance.post("/api/users/signup", {
          name: formData.name,
          email: formData.email,
          password: formData.password,
          role: "USER",
          avatar: "https://i.pravatar.cc/150?u=john",
        });

        setMessage(
          res.data?.message || "Account created successfully. Please login."
        );

        setIsSignUp(false);

        setFormData({
          name: "",
          email: formData.email,
          password: "",
        });
      } else {
        const res = await axiosInstance.post("/api/users/login", {
          email: formData.email,
          password: formData.password,
        });

        login(res.data);

        router.push("/");
      }
    } catch (err: any) {
      console.error("Authentication error:", err);

      const errorMessage =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        "Something went wrong. Please try again.";

      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleForgotPassword = async (
    e: React.FormEvent<HTMLFormElement>
  ) => {
    e.preventDefault();

    setIsLoading(true);
    setError("");
    setMessage("");

    try {
      const res = await axiosInstance.post("/api/users/forgot-password", {
        email: formData.email,
      });

      setMessage(
        res.data?.message ||
          "If the email is registered, a password reset link has been sent."
      );
    } catch (err: any) {
      console.error("Forgot password error:", err);

      const errorMessage =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        "Unable to process password reset request.";

      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="flex justify-center mb-6">
          <div className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <FolderKanban className="h-6 w-6" />
            </div>

            <span className="text-2xl font-bold">ProjectHub</span>
          </div>
        </div>

        <Card>
          <CardHeader className="text-center">
            <CardTitle className="text-2xl">
              {showForgotPassword
                ? "Reset your password"
                : isSignUp
                ? "Create your account"
                : "Welcome back"}
            </CardTitle>

            <CardDescription>
              {showForgotPassword
                ? "Enter your email to reset your password"
                : isSignUp
                ? "Create an account to get started"
                : "Log in to your account to continue"}
            </CardDescription>
          </CardHeader>

          <CardContent>
            {/* Error Message */}
            {error && (
              <div className="mb-4 flex items-start gap-2 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-600">
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Success Message */}
            {message && (
              <div className="mb-4 rounded-md border border-green-200 bg-green-50 p-3 text-sm text-green-600">
                {message}
              </div>
            )}

            {/* Forgot Password */}
            {showForgotPassword ? (
              <form onSubmit={handleForgotPassword} className="space-y-4">
                <div className="space-y-2">
                  <label
                    htmlFor="forgot-email"
                    className="text-sm font-medium"
                  >
                    Email
                  </label>

                  <Input
                    id="forgot-email"
                    name="email"
                    type="email"
                    placeholder="Enter your email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                  />
                </div>

                <Button
                  type="submit"
                  className="w-full"
                  disabled={isLoading}
                >
                  {isLoading ? "Sending..." : "Send Reset Link"}

                  {!isLoading && <ArrowRight className="ml-2 h-4 w-4" />}
                </Button>

                <button
                  type="button"
                  onClick={() => {
                    setShowForgotPassword(false);
                    setError("");
                    setMessage("");
                  }}
                  className="w-full text-sm text-muted-foreground hover:text-foreground"
                >
                  Back to login
                </button>
              </form>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                {/* Name */}
                {isSignUp && (
                  <div className="space-y-2">
                    <label htmlFor="name" className="text-sm font-medium">
                      Name
                    </label>

                    <Input
                      id="name"
                      name="name"
                      type="text"
                      placeholder="Enter your name"
                      value={formData.name}
                      onChange={handleChange}
                      required={isSignUp}
                    />
                  </div>
                )}

                {/* Email */}
                <div className="space-y-2">
                  <label htmlFor="email" className="text-sm font-medium">
                    Email
                  </label>

                  <Input
                    id="email"
                    name="email"
                    type="email"
                    placeholder="Enter your email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                  />
                </div>

                {/* Password */}
                <div className="space-y-2">
                  <label htmlFor="password" className="text-sm font-medium">
                    Password
                  </label>

                  <div className="relative">
                    <Input
                      id="password"
                      name="password"
                      type={showPassword ? "text" : "password"}
                      placeholder="Enter your password"
                      value={formData.password}
                      onChange={handleChange}
                      required
                      className="pr-10"
                    />

                    <button
                      type="button"
                      onClick={() => setShowPassword((prev) => !prev)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                      aria-label={
                        showPassword ? "Hide password" : "Show password"
                      }
                    >
                      {showPassword ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <Eye className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                </div>

                {/* Forgot Password */}
                {!isSignUp && (
                  <div className="flex justify-end">
                    <button
                      type="button"
                      onClick={() => {
                        setShowForgotPassword(true);
                        setError("");
                        setMessage("");
                      }}
                      className="text-sm text-primary hover:underline"
                    >
                      Forgot password?
                    </button>
                  </div>
                )}

                {/* Submit */}
                <Button
                  type="submit"
                  className="w-full"
                  disabled={isLoading}
                >
                  {isLoading
                    ? isSignUp
                      ? "Creating account..."
                      : "Logging in..."
                    : isSignUp
                    ? "Create Account"
                    : "Login"}

                  {!isLoading && <ArrowRight className="ml-2 h-4 w-4" />}
                </Button>
              </form>
            )}

            {/* Login / Signup Switch */}
            {!showForgotPassword && (
              <div className="mt-6 text-center text-sm text-muted-foreground">
                {isSignUp ? (
                  <>
                    Already have an account?{" "}
                    <button
                      type="button"
                      onClick={() => {
                        setIsSignUp(false);
                        setError("");
                        setMessage("");
                      }}
                      className="font-medium text-primary hover:underline"
                    >
                      Login
                    </button>
                  </>
                ) : (
                  <>
                    Don&apos;t have an account?{" "}
                    <button
                      type="button"
                      onClick={() => {
                        setIsSignUp(true);
                        setError("");
                        setMessage("");
                      }}
                      className="font-medium text-primary hover:underline"
                    >
                      Sign up
                    </button>
                  </>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Footer */}
        <p className="mt-6 text-center text-xs text-muted-foreground">
          © 2026 ProjectHub. All rights reserved.
        </p>
      </div>
    </div>
  );
}