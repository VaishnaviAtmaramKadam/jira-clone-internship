"use client";

import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/components/ui/avatar";

import { Badge } from "@/components/ui/badge";
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

import {
  AlertCircle,
  CheckCircle2,
  ImagePlus,
  Lock,
  Mail,
  Save,
  Shield,
  UserRound,
} from "lucide-react";

import React, { useEffect, useState } from "react";

const MAX_IMAGE_SIZE = 2 * 1024 * 1024;

type ProfileUser = {
  id: string;
  name: string;
  email: string;
  role?: string;
  group?: string;
  avatar?: string;
  phone?: string;
  emailVerified?: boolean;
  emailNotificationsEnabled?: boolean;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
};

type ApiResponse = {
  message?: string;
  error?: string;
  verificationToken?: string;
  emailVerificationToken?: string;
  token?: string;
  verification_token?: string;
  user?: Partial<ProfileUser>;
};

type ApiError = {
  response?: {
    data?: ApiResponse | string;
  };
};

const Page = () => {
  const { user, login, logout } = useAuth();

  const currentUser = user as ProfileUser | null;

  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [avatar, setAvatar] = useState("");
  const [email, setEmail] = useState("");

  const [newEmail, setNewEmail] = useState("");

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");

  const [emailNotificationsEnabled, setEmailNotificationsEnabled] =
    useState(true);

  const [savingProfile, setSavingProfile] = useState(false);
  const [changingEmail, setChangingEmail] = useState(false);
  const [verifyingEmail, setVerifyingEmail] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);
  const [deactivating, setDeactivating] = useState(false);

  const [verificationToken, setVerificationToken] = useState("");

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  // =====================================================
  // LOAD LATEST USER FROM BACKEND
  // =====================================================

  useEffect(() => {
    if (!currentUser?.id) {
      return;
    }

    const loadUserProfile = async () => {
      try {
        const response = await axiosInstance.get(
          `/api/users/${currentUser.id}`
        );

        const latestUser = response.data as ProfileUser;

        setName(latestUser.name ?? "");
        setEmail(latestUser.email ?? "");
        setPhone(latestUser.phone ?? "");
        setAvatar(latestUser.avatar ?? "");

        setEmailNotificationsEnabled(
          latestUser.emailNotificationsEnabled ?? true
        );

        const updatedUser: ProfileUser = {
          ...currentUser,
          ...latestUser,
        };

        // Update AuthContext + localStorage
        login(updatedUser);

        localStorage.setItem(
          "user",
          JSON.stringify(updatedUser)
        );
      } catch (loadError) {
        console.error(
          "Failed to load latest user profile:",
          loadError
        );

        // Fallback to current user data
        setName(currentUser.name ?? "");
        setEmail(currentUser.email ?? "");
        setPhone(currentUser.phone ?? "");
        setAvatar(currentUser.avatar ?? "");

        setEmailNotificationsEnabled(
          currentUser.emailNotificationsEnabled ?? true
        );
      }
    };

    loadUserProfile();

    // We only want to reload when user ID changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentUser?.id]);

  // =====================================================
  // MESSAGES
  // =====================================================

  const clearMessages = () => {
    setMessage("");
    setError("");
  };

  const getErrorMessage = (
    error: unknown
  ): string => {
    const apiError = error as ApiError;

    const data = apiError.response?.data;

    if (
      typeof data === "string" &&
      data.trim()
    ) {
      return data;
    }

    if (
      data &&
      typeof data === "object"
    ) {
      if (
        typeof data.message === "string"
      ) {
        return data.message;
      }

      if (
        typeof data.error === "string"
      ) {
        return data.error;
      }
    }

    return "Something went wrong. Please try again.";
  };

  // =====================================================
  // PROFILE PICTURE
  // =====================================================

  const handleImageChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    clearMessages();

    const file =
      event.target.files?.[0];

    if (!file) {
      return;
    }

    const allowedTypes = [
      "image/jpeg",
      "image/png",
    ];

    if (
      !allowedTypes.includes(
        file.type
      )
    ) {
      setError(
        "Unsupported image format. Please select JPG or PNG."
      );

      event.target.value = "";

      return;
    }

    if (
      file.size >
      MAX_IMAGE_SIZE
    ) {
      setError(
        "Profile picture must be smaller than 2 MB."
      );

      event.target.value = "";

      return;
    }

    const reader =
      new FileReader();

    reader.onload = () => {
      if (
        typeof reader.result === "string"
      ) {
        setAvatar(
          reader.result
        );

        setMessage(
          "Profile picture selected. Click Save Changes to save it."
        );
      }
    };

    reader.onerror = () => {
      setError(
        "Failed to read the selected image."
      );
    };

    reader.readAsDataURL(file);
  };

  // =====================================================
  // SAVE PROFILE
  // =====================================================

  const handleSaveProfile =
    async () => {
      if (!currentUser?.id) {
        setError(
          "User information is not available."
        );

        return;
      }

      clearMessages();

      const trimmedName =
        name.trim();

      const trimmedPhone =
        phone.trim();

      if (!trimmedName) {
        setError(
          "Full name is required."
        );

        return;
      }

      if (
        trimmedPhone &&
        !/^[0-9]{10}$/.test(
          trimmedPhone
        )
      ) {
        setError(
          "Phone number must contain exactly 10 digits."
        );

        return;
      }

      try {
        setSavingProfile(true);

        const response =
          await axiosInstance.put(
            `/api/users/${currentUser.id}`,
            {
              name: trimmedName,
              phone: trimmedPhone,
              avatar,
              emailNotificationsEnabled:
                emailNotificationsEnabled,
            }
          );

        const responseData =
          response.data as
            | ApiResponse
            | ProfileUser;

        const updatedUser =
          (
            responseData as ApiResponse
          ).user ??
          (responseData as ProfileUser);

        const updatedProfile: ProfileUser =
          {
            ...currentUser,

            id:
              updatedUser.id ??
              currentUser.id,

            name:
              updatedUser.name ??
              trimmedName,

            email:
              updatedUser.email ??
              currentUser.email,

            role:
              updatedUser.role ??
              currentUser.role,

            group:
              updatedUser.group ??
              currentUser.group,

            avatar:
              updatedUser.avatar !==
              undefined
                ? updatedUser.avatar
                : avatar,

            phone:
              updatedUser.phone !==
              undefined
                ? updatedUser.phone
                : trimmedPhone,

            emailVerified:
              updatedUser.emailVerified ??
              currentUser.emailVerified,

            emailNotificationsEnabled:
              updatedUser.emailNotificationsEnabled !==
              undefined
                ? updatedUser.emailNotificationsEnabled
                : emailNotificationsEnabled,

            active:
              updatedUser.active ??
              currentUser.active,

            createdAt:
              updatedUser.createdAt ??
              currentUser.createdAt,

            updatedAt:
              updatedUser.updatedAt ??
              currentUser.updatedAt,
          };

        // IMPORTANT:
        // Save latest user in localStorage
        localStorage.setItem(
          "user",
          JSON.stringify(updatedProfile)
        );

        // Update AuthContext
        login(updatedProfile);

        // Update UI state
        setName(
          updatedProfile.name
        );

        setPhone(
          updatedProfile.phone ??
          ""
        );

        setAvatar(
          updatedProfile.avatar ??
          ""
        );

        setEmail(
          updatedProfile.email
        );

        setEmailNotificationsEnabled(
          updatedProfile.emailNotificationsEnabled ??
          false
        );

        setMessage(
          updatedProfile.emailNotificationsEnabled
            ? "Profile updated successfully. Email notifications are ON."
            : "Profile updated successfully. Email notifications are OFF."
        );

      } catch (error: unknown) {
        console.error(
          "Failed to update profile:",
          error
        );

        setError(
          getErrorMessage(error)
        );
      } finally {
        setSavingProfile(false);
      }
    };

  // =====================================================
  // REQUEST EMAIL VERIFICATION
  // =====================================================

  const handleChangeEmail =
    async () => {
      if (!currentUser?.id) {
        setError(
          "User information is not available."
        );

        return;
      }

      clearMessages();

      setVerificationToken("");

      const emailValue =
        newEmail
          .trim()
          .toLowerCase();

      if (!emailValue) {
        setError(
          "Please enter a new email address."
        );

        return;
      }

      const emailRegex =
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      if (
        !emailRegex.test(
          emailValue
        )
      ) {
        setError(
          "Please enter a valid email address."
        );

        return;
      }

      const currentEmail =
        (
          currentUser.email ??
          ""
        )
          .trim()
          .toLowerCase();

      if (
        emailValue ===
        currentEmail
      ) {
        setError(
          "New email must be different from current email."
        );

        return;
      }

      try {
        setChangingEmail(true);

        const response =
          await axiosInstance.post(
            `/api/users/${currentUser.id}/change-email`,
            {
              email: emailValue,
            }
          );

        const data =
          response.data as
            | ApiResponse
            | string;

        console.log(
          "Email verification response:",
          data
        );

        let token = "";

        let successMessage =
          "Verification request sent successfully.";

        if (
          data &&
          typeof data === "object"
        ) {
          token =
            data.verificationToken ||
            data.emailVerificationToken ||
            data.token ||
            data.verification_token ||
            "";

          if (
            data.message &&
            typeof data.message === "string"
          ) {
            successMessage =
              data.message;
          }
        }

        if (
          typeof data === "string"
        ) {
          successMessage =
            data;

          const tokenMatch =
            data.match(
              /(?:Email verification token generated:\s*|Verification token:\s*|token:\s*)(.+)/i
            );

          if (
            tokenMatch?.[1]
          ) {
            token =
              tokenMatch[1].trim();
          }
        }

        if (token) {
          setVerificationToken(
            token
          );

          setMessage(
            "Verification token generated successfully. Please verify your new email."
          );
        } else {
          setMessage(
            successMessage
          );
        }

      } catch (error: unknown) {
        console.error(
          "Failed to request email verification:",
          error
        );

        setError(
          getErrorMessage(error)
        );
      } finally {
        setChangingEmail(
          false
        );
      }
    };

  // =====================================================
  // VERIFY EMAIL
  // =====================================================

  const handleVerifyEmail =
    async () => {
      const token =
        verificationToken.trim();

      if (!token) {
        setError(
          "Verification token is required."
        );

        return;
      }

      if (!currentUser?.id) {
        setError(
          "User information is not available."
        );

        return;
      }

      clearMessages();

      try {
        setVerifyingEmail(true);

        const response =
          await axiosInstance.post(
            `/api/users/verify-email/${encodeURIComponent(
              token
            )}`
          );

        const data =
          response.data as
            | ApiResponse
            | ProfileUser;

        let verifiedUser: ProfileUser =
          currentUser;

        if (
          data &&
          typeof data === "object"
        ) {
          const responseUser =
            (
              data as ApiResponse
            ).user;

          if (
            responseUser
          ) {
            verifiedUser = {
              ...currentUser,
              ...responseUser,
            };
          } else {
            verifiedUser = {
              ...currentUser,
              ...(data as ProfileUser),
            };
          }
        }

        verifiedUser = {
          ...verifiedUser,

          email:
            verifiedUser.email ||
            newEmail.trim(),

          emailVerified: true,
        };

        login(verifiedUser);

        localStorage.setItem(
          "user",
          JSON.stringify(verifiedUser)
        );

        setEmail(
          verifiedUser.email
        );

        setNewEmail("");

        setVerificationToken("");

        setMessage(
          "Email verified successfully. Your new email address is now active."
        );

      } catch (error: unknown) {
        console.error(
          "Failed to verify email:",
          error
        );

        setError(
          getErrorMessage(error)
        );
      } finally {
        setVerifyingEmail(
          false
        );
      }
    };

  // =====================================================
  // CHANGE PASSWORD
  // =====================================================

  const handleChangePassword =
    async () => {
      if (!currentUser?.id) {
        setError(
          "User information is not available."
        );

        return;
      }

      clearMessages();

      if (!currentPassword) {
        setError(
          "Current password is required."
        );

        return;
      }

      if (!newPassword) {
        setError(
          "New password is required."
        );

        return;
      }

      if (
        newPassword.length < 8
      ) {
        setError(
          "Password must contain at least 8 characters."
        );

        return;
      }

      if (
        !/[A-Z]/.test(
          newPassword
        )
      ) {
        setError(
          "Password must contain at least one uppercase letter."
        );

        return;
      }

      if (
        !/[a-z]/.test(
          newPassword
        )
      ) {
        setError(
          "Password must contain at least one lowercase letter."
        );

        return;
      }

      if (
        !/[0-9]/.test(
          newPassword
        )
      ) {
        setError(
          "Password must contain at least one number."
        );

        return;
      }

      if (
        !/[^A-Za-z0-9]/.test(
          newPassword
        )
      ) {
        setError(
          "Password must contain at least one special character."
        );

        return;
      }

      if (
        currentPassword ===
        newPassword
      ) {
        setError(
          "New password must be different from current password."
        );

        return;
      }

      try {
        setChangingPassword(
          true
        );

        await axiosInstance.post(
          `/api/users/${currentUser.id}/change-password`,
          {
            currentPassword,
            newPassword,
          }
        );

        setCurrentPassword("");
        setNewPassword("");

        setMessage(
          "Password changed successfully."
        );

      } catch (error: unknown) {
        console.error(
          "Failed to change password:",
          error
        );

        setError(
          getErrorMessage(error)
        );
      } finally {
        setChangingPassword(
          false
        );
      }
    };

  // =====================================================
  // DEACTIVATE ACCOUNT
  // =====================================================

  const handleDeactivateAccount =
    async () => {
      if (!currentUser?.id) {
        setError(
          "User information is not available."
        );

        return;
      }

      const confirmed =
        window.confirm(
          "Are you sure you want to deactivate your account? You will not be able to login until the account is activated again."
        );

      if (!confirmed) {
        return;
      }

      clearMessages();

      try {
        setDeactivating(true);

        await axiosInstance.put(
          `/api/users/${currentUser.id}/deactivate`
        );

        alert(
          "Account deactivated successfully."
        );

        logout();

      } catch (error: unknown) {
        console.error(
          "Failed to deactivate account:",
          error
        );

        setError(
          getErrorMessage(error)
        );
      } finally {
        setDeactivating(
          false
        );
      }
    };

  // =====================================================
  // NO USER
  // =====================================================

  if (!currentUser) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <div className="text-center">

          <h2 className="text-xl font-semibold text-[#172B4D]">
            User not found
          </h2>

          <p className="mt-2 text-sm text-[#5E6C84]">
            Please login again to access your profile.
          </p>

        </div>
      </div>
    );
  }

  // =====================================================
  // UI
  // =====================================================

  return (
    <div className="flex h-full flex-col overflow-auto bg-[#F4F5F7] p-6">

      {/* Header */}

      <div className="mb-8">

        <h1 className="mb-2 text-3xl font-bold text-[#172B4D]">
          Profile Settings
        </h1>

        <p className="text-[#5E6C84]">
          Manage your personal information and security
        </p>

      </div>

      {/* Success Message */}

      {message && (
        <div className="mb-6 flex items-center gap-2 rounded-md border border-green-200 bg-green-50 p-3 text-sm text-green-700">

          <CheckCircle2 className="h-4 w-4 shrink-0" />

          <span>
            {message}
          </span>

        </div>
      )}

      {/* Error Message */}

      {error && (
        <div className="mb-6 flex items-center gap-2 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">

          <AlertCircle className="h-4 w-4 shrink-0" />

          <span>
            {error}
          </span>

        </div>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">

        {/* =====================================================
            ABOUT YOU
        ===================================================== */}

        <Card className="lg:col-span-1">

          <CardHeader>

            <CardTitle className="text-[#172B4D]">
              About You
            </CardTitle>

          </CardHeader>

          <CardContent>

            <div className="space-y-5">

              {/* Avatar */}

              <div className="flex flex-col items-center">

                <Avatar className="mb-4 h-24 w-24">

                  <AvatarImage
                    src={
                      avatar ||
                      "/placeholder.svg"
                    }
                    alt={
                      name ||
                      "User"
                    }
                  />

                  <AvatarFallback>
                    {name
                      ? name
                          .charAt(0)
                          .toUpperCase()
                      : "U"}
                  </AvatarFallback>

                </Avatar>

                <h2 className="text-xl font-semibold text-[#172B4D]">
                  {name || "User"}
                </h2>

                <Badge className="mt-2">
                  {currentUser.role ||
                    "USER"}
                </Badge>

              </div>

              {/* Profile Picture */}

              <div className="border-t pt-4">

                <label
                  htmlFor="profile-picture"
                  className="flex cursor-pointer items-center justify-center gap-2 rounded-md bg-[#0052CC] px-4 py-2 text-sm font-medium text-white hover:bg-[#0747A6]"
                >

                  <ImagePlus className="h-4 w-4" />

                  Change Profile Picture

                </label>

                <input
                  id="profile-picture"
                  type="file"
                  accept="image/png,image/jpeg"
                  className="hidden"
                  onChange={
                    handleImageChange
                  }
                />

                <p className="mt-2 text-center text-xs text-[#6B778C]">
                  JPG or PNG only · Maximum 2 MB
                </p>

              </div>

              {/* Email */}

              <div className="flex items-center gap-3 border-t pt-4 text-sm">

                <Mail className="h-4 w-4 shrink-0 text-[#5E6C84]" />

                <span className="break-all text-[#172B4D]">
                  {currentUser.email}
                </span>

              </div>

              {/* Phone */}

              <div className="flex items-center gap-3 text-sm">

                <span className="text-[#5E6C84]">
                  Phone:
                </span>

                <span className="text-[#172B4D]">
                  {phone ||
                    "Not added"}
                </span>

              </div>

              {/* Group */}

              <div className="flex items-center gap-3 text-sm">

                <span className="text-[#5E6C84]">
                  Group:
                </span>

                <Badge variant="outline">
                  {currentUser.group ||
                    "Not assigned"}
                </Badge>

              </div>

              {/* Email Status */}

              <div className="flex items-center justify-between border-t pt-4 text-sm">

                <span className="text-[#5E6C84]">
                  Email Status
                </span>

                <Badge
                  variant={
                    currentUser.emailVerified
                      ? "default"
                      : "outline"
                  }
                >
                  {currentUser.emailVerified
                    ? "Verified"
                    : "Not Verified"}
                </Badge>

              </div>

              {/* Email Notification Status */}

              <div className="flex items-center justify-between border-t pt-4 text-sm">

                <span className="text-[#5E6C84]">
                  Email Notifications
                </span>

                <Badge
                  variant={
                    emailNotificationsEnabled
                      ? "default"
                      : "outline"
                  }
                >
                  {emailNotificationsEnabled
                    ? "ON"
                    : "OFF"}
                </Badge>

              </div>

            </div>

          </CardContent>

        </Card>

        {/* =====================================================
            RIGHT SIDE
        ===================================================== */}

        <div className="space-y-6 lg:col-span-2">

          {/* =================================================
              PERSONAL INFORMATION
          ================================================= */}

          <Card>

            <CardHeader>

              <CardTitle className="flex items-center gap-2 text-[#172B4D]">

                <UserRound className="h-5 w-5" />

                Personal Information

              </CardTitle>

              <CardDescription>
                Update your name and contact details
              </CardDescription>

            </CardHeader>

            <CardContent>

              <div className="space-y-4">

                {/* Name */}

                <div>

                  <label className="mb-1 block text-sm font-semibold text-[#172B4D]">
                    Full Name
                  </label>

                  <Input
                    value={name}
                    onChange={(e) =>
                      setName(
                        e.target.value
                      )
                    }
                    placeholder="Enter your full name"
                    className="focus-visible:ring-[#0052CC]"
                  />

                </div>

                {/* Phone */}

                <div>

                  <label className="mb-1 block text-sm font-semibold text-[#172B4D]">
                    Phone Number
                  </label>

                  <Input
                    value={phone}
                    onChange={(e) =>
                      setPhone(
                        e.target.value
                          .replace(
                            /\D/g,
                            ""
                          )
                          .slice(
                            0,
                            10
                          )
                      )
                    }
                    placeholder="10 digit phone number"
                    maxLength={10}
                    inputMode="numeric"
                    className="focus-visible:ring-[#0052CC]"
                  />

                </div>

                {/* Current Email */}

                <div>

                  <label className="mb-1 block text-sm font-semibold text-[#172B4D]">
                    Current Email
                  </label>

                  <Input
                    type="email"
                    value={email}
                    disabled
                    className="bg-gray-100"
                  />

                </div>

                {/* Role and Team */}

                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">

                  <div>

                    <label className="mb-1 block text-sm font-semibold text-[#172B4D]">
                      Role
                    </label>

                    <Input
                      disabled
                      value={
                        currentUser.role ||
                        ""
                      }
                      className="bg-gray-100"
                    />

                  </div>

                  <div>

                    <label className="mb-1 block text-sm font-semibold text-[#172B4D]">
                      Team
                    </label>

                    <Input
                      disabled
                      value={
                        currentUser.group ||
                        ""
                      }
                      className="bg-gray-100"
                    />

                  </div>

                </div>

                {/* =================================================
                    EMAIL NOTIFICATIONS
                ================================================= */}

                <div className="rounded-lg border bg-gray-50 p-4">

                  <div className="flex items-center justify-between gap-4">

                    <div>

                      <h3 className="text-sm font-semibold text-[#172B4D]">
                        Email Notifications
                      </h3>

                      <p className="mt-1 text-xs text-[#5E6C84]">
                        Receive task assignments, status changes,
                        due-date reminders and other important
                        Jira Clone notifications by email.
                      </p>

                    </div>

                    <label className="relative inline-flex cursor-pointer items-center">

                      <input
                        type="checkbox"
                        className="peer sr-only"
                        checked={
                          emailNotificationsEnabled
                        }
                        onChange={(e) =>
                          setEmailNotificationsEnabled(
                            e.target.checked
                          )
                        }
                      />

                      <div className="h-6 w-11 rounded-full bg-gray-300 peer-checked:bg-[#0052CC] peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-[#0052CC]/30 after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:bg-white after:transition-all after:content-[''] peer-checked:after:translate-x-full">
                      </div>

                    </label>

                  </div>

                  <div className="mt-3 text-xs font-medium">

                    {emailNotificationsEnabled ? (
                      <span className="text-green-600">
                        Email notifications are ON
                      </span>
                    ) : (
                      <span className="text-gray-500">
                        Email notifications are OFF
                      </span>
                    )}

                  </div>

                </div>

                {/* Save */}

                <div className="flex justify-end pt-4">

                  <Button
                    onClick={
                      handleSaveProfile
                    }
                    disabled={
                      savingProfile
                    }
                    className="bg-[#0052CC] text-white hover:bg-[#0747A6]"
                  >

                    <Save className="mr-2 h-4 w-4" />

                    {savingProfile
                      ? "Saving..."
                      : "Save Changes"}

                  </Button>

                </div>

              </div>

            </CardContent>

          </Card>

          {/* =================================================
              CHANGE EMAIL
          ================================================= */}

          <Card>

            <CardHeader>

              <CardTitle className="flex items-center gap-2 text-[#172B4D]">

                <Mail className="h-5 w-5" />

                Change Email

              </CardTitle>

              <CardDescription>
                Email changes require verification before the new address becomes active.
              </CardDescription>

            </CardHeader>

            <CardContent>

              <div className="space-y-4">

                <div>

                  <label className="mb-1 block text-sm font-semibold text-[#172B4D]">
                    New Email Address
                  </label>

                  <Input
                    type="email"
                    value={newEmail}
                    onChange={(e) => {
                      setNewEmail(
                        e.target.value
                      );

                      setVerificationToken("");

                      clearMessages();
                    }}
                    placeholder="Enter new email"
                  />

                </div>

                <div className="flex justify-end">

                  <Button
                    type="button"
                    onClick={
                      handleChangeEmail
                    }
                    disabled={
                      changingEmail ||
                      !newEmail.trim()
                    }
                    className="bg-[#0052CC] text-white hover:bg-[#0747A6]"
                  >

                    <Mail className="mr-2 h-4 w-4" />

                    {changingEmail
                      ? "Processing..."
                      : "Request Verification"}

                  </Button>

                </div>

                {verificationToken && (

                  <div className="rounded-md border border-blue-200 bg-blue-50 p-4">

                    <div className="flex items-center gap-2">

                      <CheckCircle2 className="h-4 w-4 text-blue-600" />

                      <p className="text-sm font-semibold text-[#172B4D]">
                        Email Verification
                      </p>

                    </div>

                    <p className="mt-2 text-xs text-[#5E6C84]">
                      Enter/use the verification token generated for your new email address.
                    </p>

                    <Input
                      value={
                        verificationToken
                      }
                      onChange={(e) =>
                        setVerificationToken(
                          e.target.value
                        )
                      }
                      className="mt-3 bg-white font-mono text-xs"
                      placeholder="Enter verification token"
                    />

                    <div className="mt-3 flex justify-end">

                      <Button
                        type="button"
                        onClick={
                          handleVerifyEmail
                        }
                        disabled={
                          verifyingEmail ||
                          !verificationToken.trim()
                        }
                        className="bg-green-600 text-white hover:bg-green-700"
                      >

                        <CheckCircle2 className="mr-2 h-4 w-4" />

                        {verifyingEmail
                          ? "Verifying..."
                          : "Verify Email"}

                      </Button>

                    </div>

                    <p className="mt-3 text-xs text-[#5E6C84]">
                      Development mode: this token is used to verify the new email.
                    </p>

                  </div>

                )}

              </div>

            </CardContent>

          </Card>

          {/* =================================================
              CHANGE PASSWORD
          ================================================= */}

          <Card>

            <CardHeader>

              <CardTitle className="flex items-center gap-2 text-[#172B4D]">

                <Lock className="h-5 w-5" />

                Change Password

              </CardTitle>

              <CardDescription>
                Current password is required before changing your password.
              </CardDescription>

            </CardHeader>

            <CardContent>

              <div className="space-y-4">

                <div>

                  <label className="mb-1 block text-sm font-semibold text-[#172B4D]">
                    Current Password
                  </label>

                  <Input
                    type="password"
                    value={
                      currentPassword
                    }
                    onChange={(e) =>
                      setCurrentPassword(
                        e.target.value
                      )
                    }
                    placeholder="Enter current password"
                  />

                </div>

                <div>

                  <label className="mb-1 block text-sm font-semibold text-[#172B4D]">
                    New Password
                  </label>

                  <Input
                    type="password"
                    value={newPassword}
                    onChange={(e) =>
                      setNewPassword(
                        e.target.value
                      )
                    }
                    placeholder="Enter new password"
                  />

                </div>

                <div className="rounded-md border bg-gray-50 p-3">

                  <p className="mb-2 text-sm font-semibold text-[#172B4D]">
                    Password requirements
                  </p>

                  <ul className="space-y-1 text-xs text-[#5E6C84]">

                    <li>
                      • Minimum 8 characters
                    </li>

                    <li>
                      • At least one uppercase letter
                    </li>

                    <li>
                      • At least one lowercase letter
                    </li>

                    <li>
                      • At least one number
                    </li>

                    <li>
                      • At least one special character
                    </li>

                  </ul>

                </div>

                <div className="flex justify-end">

                  <Button
                    onClick={
                      handleChangePassword
                    }
                    disabled={
                      changingPassword ||
                      !currentPassword ||
                      !newPassword
                    }
                    className="bg-[#0052CC] text-white hover:bg-[#0747A6]"
                  >

                    <Shield className="mr-2 h-4 w-4" />

                    {changingPassword
                      ? "Changing..."
                      : "Change Password"}

                  </Button>

                </div>

              </div>

            </CardContent>

          </Card>

          {/* =================================================
              ACTIVITY
          ================================================= */}

          <Card>

            <CardHeader>

              <CardTitle className="text-[#172B4D]">
                Activity
              </CardTitle>

              <CardDescription>
                Your account activity information
              </CardDescription>

            </CardHeader>

            <CardContent>

              <div className="space-y-3">

                <div className="flex justify-between text-sm">

                  <span className="text-[#5E6C84]">
                    Account Created
                  </span>

                  <span className="font-semibold text-[#172B4D]">

                    {currentUser.createdAt
                      ? new Date(
                          currentUser.createdAt
                        ).toLocaleDateString()
                      : "N/A"}

                  </span>

                </div>

                <div className="flex justify-between border-t pt-3 text-sm">

                  <span className="text-[#5E6C84]">
                    Account Status
                  </span>

                  <Badge>

                    {currentUser.active ===
                    false
                      ? "Inactive"
                      : "Active"}

                  </Badge>

                </div>

              </div>

            </CardContent>

          </Card>

          {/* =================================================
              DANGER ZONE
          ================================================= */}

          <Card className="border-red-200">

            <CardHeader>

              <CardTitle className="text-red-600">
                Danger Zone
              </CardTitle>

              <CardDescription>
                Deactivating your account prevents login while preserving historical activity.
              </CardDescription>

            </CardHeader>

            <CardContent>

              <Button
                variant="destructive"
                onClick={
                  handleDeactivateAccount
                }
                disabled={
                  deactivating
                }
              >

                {deactivating
                  ? "Deactivating..."
                  : "Deactivate Account"}

              </Button>

            </CardContent>

          </Card>

        </div>

      </div>

    </div>
  );
};

export default Page;