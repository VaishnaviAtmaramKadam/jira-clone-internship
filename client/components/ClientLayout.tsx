"use client";

import React, { useEffect } from "react";
import {
  usePathname,
  useRouter,
} from "next/navigation";

import { useAuth } from "@/lib/AuthContext";
import Sidebar from "./Sidebar";

const ClientLayout = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const pathname = usePathname();
  const router = useRouter();

  const {
    user,
    isAuthReady,
  } = useAuth();

  useEffect(() => {
    // Wait until authentication is loaded
    if (!isAuthReady) {
      return;
    }

    const publicPages = [
      "/login",
      "/setup-project",
    ];

    const isPublic =
      publicPages.includes(pathname);

    // User is NOT logged in
    // and trying to access protected page
    if (!user && !isPublic) {
      router.replace("/login");
    }
  }, [
    user,
    isAuthReady,
    pathname,
    router,
  ]);

  // Wait until authentication check is complete
  if (!isAuthReady) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-white">
        <div className="text-sm text-[#6B778C]">
          Loading...
        </div>
      </div>
    );
  }

  const isAuthPage =
    pathname === "/login" ||
    pathname === "/setup-project";

  // Login and setup pages
  // don't show sidebar
  if (isAuthPage) {
    return <>{children}</>;
  }

  // Protected application pages
  return (
    <div className="flex min-h-screen bg-white">
      <Sidebar />

      <main className="flex-1 overflow-x-hidden">
        {children}
      </main>
    </div>
  );
};

export default ClientLayout;