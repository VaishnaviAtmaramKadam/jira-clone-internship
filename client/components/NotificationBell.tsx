"use client";

import { Bell, Check, CheckCheck } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import axiosInstance from "@/lib/Axiosinstance";
import { useAuth } from "@/lib/AuthContext";

interface Notification {
  id: string;
  userId: string;
  projectId?: string;
  issueId?: string;
  type: string;
  message: string;
  read: boolean;
  eventKey?: string;
  createdAt: string;
}

const NotificationBell = () => {
  const { user } = useAuth();

  const [notifications, setNotifications] =
    useState<Notification[]>([]);

  const [unreadCount, setUnreadCount] =
    useState(0);

  const [showNotifications, setShowNotifications] =
    useState(false);

  const [loading, setLoading] =
    useState(false);

  // =====================================================
  // FETCH NOTIFICATIONS
  // =====================================================
  const fetchNotifications = useCallback(async () => {
    if (!user?.id) {
      console.log(
        "NotificationBell: User ID not available"
      );
      return;
    }

    // IMPORTANT DEBUG
    console.log(
      "CURRENT LOGGED-IN USER ID:",
      user.id
    );

    try {
      // =================================================
      // GET ALL NOTIFICATIONS
      // =================================================
      const notificationsResponse =
        await axiosInstance.get(
          `/api/notifications/user/${user.id}`,
          {
            headers: {
              "Cache-Control": "no-cache",
              Pragma: "no-cache",
            },
          }
        );

      const notificationData =
        Array.isArray(notificationsResponse.data)
          ? notificationsResponse.data
          : [];

      console.log(
        "Notifications received:",
        notificationData
      );

      // =================================================
      // DEBUG: SHOW USER IDs OF RECEIVED NOTIFICATIONS
      // =================================================
      console.log(
        "Notification User IDs:",
        notificationData.map(
          (notification: Notification) =>
            notification.userId
        )
      );

      // =================================================
      // GET UNREAD COUNT FROM BACKEND
      // =================================================
      const countResponse =
        await axiosInstance.get(
          `/api/notifications/user/${user.id}/unread-count`,
          {
            headers: {
              "Cache-Control": "no-cache",
              Pragma: "no-cache",
            },
          }
        );

      const backendUnreadCount =
        Number(countResponse.data) || 0;

      console.log(
        "Backend unread count:",
        backendUnreadCount
      );

      // =================================================
      // DEBUG: SHOW UNREAD NOTIFICATIONS
      // =================================================
      const unreadNotifications =
        notificationData.filter(
          (notification: Notification) =>
            notification.read === false
        );

      console.log(
        "Unread notifications:",
        unreadNotifications
      );

      console.log(
        "Unread notifications count from response:",
        unreadNotifications.length
      );

      setNotifications(
        notificationData
      );

      setUnreadCount(
        backendUnreadCount
      );

    } catch (error: any) {
      console.error(
        "Failed to fetch notifications:",
        error
      );

      if (error?.response) {
        console.error(
          "Notification API status:",
          error.response.status
        );

        console.error(
          "Notification API response:",
          error.response.data
        );
      }
    }
  }, [user?.id]);

  // =====================================================
  // LOAD NOTIFICATIONS
  // =====================================================
  useEffect(() => {
    if (!user?.id) {
      return;
    }

    fetchNotifications();

    const interval =
      setInterval(() => {
        fetchNotifications();
      }, 5000);

    return () => {
      clearInterval(interval);
    };
  }, [
    user?.id,
    fetchNotifications,
  ]);

  // =====================================================
  // MARK SINGLE NOTIFICATION AS READ
  // =====================================================
  const markAsRead = async (
    notificationId: string
  ) => {
    try {
      await axiosInstance.put(
        `/api/notifications/${notificationId}/read`
      );

      setNotifications(
        (previous) =>
          previous.map(
            (notification) =>
              notification.id === notificationId
                ? {
                    ...notification,
                    read: true,
                  }
                : notification
          )
      );

      setUnreadCount(
        (previous) =>
          previous > 0
            ? previous - 1
            : 0
      );

    } catch (error) {
      console.error(
        "Failed to mark notification as read:",
        error
      );
    }
  };

  // =====================================================
  // MARK ALL AS READ
  // =====================================================
  const markAllAsRead = async () => {
    if (!user?.id || unreadCount === 0) {
      return;
    }

    setLoading(true);

    try {
      await axiosInstance.put(
        `/api/notifications/user/${user.id}/read-all`
      );

      setNotifications(
        (previous) =>
          previous.map(
            (notification) => ({
              ...notification,
              read: true,
            })
          )
      );

      setUnreadCount(0);

    } catch (error) {
      console.error(
        "Failed to mark all notifications as read:",
        error
      );
    } finally {
      setLoading(false);
    }
  };

  // =====================================================
  // UI
  // =====================================================
  return (
    <div className="relative">

      {/* =================================================
          NOTIFICATION BUTTON
          ================================================= */}
      <button
        type="button"
        onClick={() =>
          setShowNotifications(
            (previous) => !previous
          )
        }
        className="relative flex h-9 w-9 items-center justify-center rounded hover:bg-[#EBECF0]"
        title="Notifications"
      >
        <Bell className="h-5 w-5 text-[#42526E]" />

        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
            {unreadCount > 99
              ? "99+"
              : unreadCount}
          </span>
        )}
      </button>

      {/* =================================================
          NOTIFICATION PANEL
          ================================================= */}
      {showNotifications && (
        <div className="absolute left-10 top-0 z-[100] w-80 rounded-lg border border-[#DFE1E6] bg-white shadow-xl">

          {/* HEADER */}
          <div className="flex items-center justify-between border-b px-4 py-3">

            <div>
              <h3 className="text-sm font-semibold text-[#172B4D]">
                Notifications
              </h3>

              {unreadCount > 0 && (
                <p className="text-xs text-[#6B778C]">
                  {unreadCount} unread
                </p>
              )}
            </div>

            {unreadCount > 0 && (
              <button
                type="button"
                onClick={markAllAsRead}
                disabled={loading}
                className="flex items-center gap-1 text-xs text-[#0052CC] hover:underline disabled:opacity-50"
              >
                <CheckCheck className="h-3.5 w-3.5" />
                Mark all read
              </button>
            )}

          </div>

          {/* NOTIFICATIONS LIST */}
          <div className="max-h-96 overflow-y-auto">

            {notifications.length === 0 ? (
              <div className="px-4 py-8 text-center">

                <Bell className="mx-auto mb-2 h-8 w-8 text-[#B3BAC5]" />

                <p className="text-sm text-[#6B778C]">
                  No notifications
                </p>

              </div>
            ) : (
              notifications.map(
                (notification) => (
                  <div
                    key={notification.id}
                    className={`border-b px-4 py-3 ${
                      notification.read
                        ? "bg-white"
                        : "bg-[#F4F8FF]"
                    }`}
                  >

                    <div className="flex gap-3">

                      <div className="flex-1">

                        <p
                          className={`text-sm ${
                            notification.read
                              ? "text-[#42526E]"
                              : "font-semibold text-[#172B4D]"
                          }`}
                        >
                          {notification.message}
                        </p>

                        <p className="mt-1 text-xs text-[#6B778C]">
                          {notification.createdAt
                            ? new Date(
                                notification.createdAt
                              ).toLocaleString()
                            : ""}
                        </p>

                      </div>

                      {!notification.read && (
                        <button
                          type="button"
                          onClick={() =>
                            markAsRead(
                              notification.id
                            )
                          }
                          className="flex h-7 w-7 shrink-0 items-center justify-center rounded hover:bg-[#EBECF0]"
                          title="Mark as read"
                        >
                          <Check className="h-4 w-4 text-[#0052CC]" />
                        </button>
                      )}

                    </div>

                  </div>
                )
              )
            )}

          </div>

        </div>
      )}

    </div>
  );
};

export default NotificationBell;