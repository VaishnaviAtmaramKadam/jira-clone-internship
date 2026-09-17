"use client";

import React, {
  createContext,
  useContext,
  useEffect,
  useState,
} from "react";

type User = {
  id: string;
  name: string;
  email: string;
  role?: string;
  group?: string;
  avatar?: string;
};

type Project = {
  id: string;
  name: string;
  [key: string]: any;
};

type AuthContextType = {
  user: User | null;

  selectedProject: Project | null;
  setSelectedProject: (project: Project | null) => void;

  isAuthReady: boolean;

  login: (user: User) => void;
  logout: () => void;
};

const AuthContext =
  createContext<AuthContextType | undefined>(
    undefined
  );

export const AuthProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const [user, setUser] =
    useState<User | null>(null);

  const [selectedProject, setSelectedProjectState] =
    useState<Project | null>(null);

  const [isAuthReady, setIsAuthReady] =
    useState(false);

  // =========================================================
  // LOAD USER + PROJECT FROM LOCAL STORAGE
  // =========================================================

  useEffect(() => {
    try {
      // Load User
      const storedUser =
        localStorage.getItem("user");

      if (storedUser) {
        setUser(JSON.parse(storedUser));
      }

      // Load Selected Project
      const storedProject =
        localStorage.getItem("selectedProject");

      if (storedProject) {
        setSelectedProjectState(
          JSON.parse(storedProject)
        );
      }
    } catch (error) {
      console.error(
        "Failed to load auth data:",
        error
      );

      localStorage.removeItem("user");
      localStorage.removeItem("selectedProject");

      setUser(null);
      setSelectedProjectState(null);
    } finally {
      setIsAuthReady(true);
    }
  }, []);

  // =========================================================
  // LOGIN
  // =========================================================

  const login = (loggedInUser: User) => {
    setUser(loggedInUser);

    localStorage.setItem(
      "user",
      JSON.stringify(loggedInUser)
    );
  };

  // =========================================================
  // SELECT PROJECT
  // =========================================================

  const setSelectedProject = (
    project: Project | null
  ) => {
    setSelectedProjectState(project);

    if (project) {
      localStorage.setItem(
        "selectedProject",
        JSON.stringify(project)
      );
    } else {
      localStorage.removeItem(
        "selectedProject"
      );
    }
  };

  // =========================================================
  // LOGOUT
  // =========================================================

  const logout = () => {
    setUser(null);
    setSelectedProjectState(null);

    localStorage.removeItem("user");

    localStorage.removeItem(
      "selectedProject"
    );
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        selectedProject,
        setSelectedProject,
        isAuthReady,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

// ===========================================================
// USE AUTH
// ===========================================================

export const useAuth = () => {
  const context =
    useContext(AuthContext);

  if (!context) {
    throw new Error(
      "useAuth must be used inside AuthProvider"
    );
  }

  return context;
};