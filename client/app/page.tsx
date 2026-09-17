"use client";

import KanbanBoard from "@/components/KanbanBoard";
import { AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@radix-ui/react-avatar";
import {
  ChevronRight,
  MoreHorizontal,
  Share2,
  RefreshCw,
  Maximize,
  Minimize,
} from "lucide-react";
import { Suspense, useEffect, useRef, useState } from "react";

export default function Home() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);

  const menuRef = useRef<HTMLDivElement>(null);

  // =========================================================
  // CLOSE MENU WHEN CLICKING OUTSIDE
  // =========================================================

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        menuRef.current &&
        !menuRef.current.contains(
          event.target as Node
        )
      ) {
        setMenuOpen(false);
      }
    };

    document.addEventListener(
      "mousedown",
      handleClickOutside
    );

    return () => {
      document.removeEventListener(
        "mousedown",
        handleClickOutside
      );
    };
  }, []);

  // =========================================================
  // SHARE BOARD
  // =========================================================

  const handleShare = async () => {
    const boardUrl = window.location.href;

    try {
      if (navigator.share) {
        await navigator.share({
          title: "Jira Clone - Kanban Board",
          text: "Check this Kanban Board",
          url: boardUrl,
        });
      } else {
        await navigator.clipboard.writeText(
          boardUrl
        );

        alert("Board link copied successfully!");
      }
    } catch (error: any) {
      // User cancelled the native share dialog.
      if (
        error?.name === "AbortError"
      ) {
        return;
      }

      console.error(
        "Share failed:",
        error
      );

      try {
        await navigator.clipboard.writeText(
          boardUrl
        );

        alert("Board link copied successfully!");
      } catch {
        alert(
          "Unable to share or copy the board link."
        );
      }
    }
  };

  // =========================================================
  // REFRESH BOARD
  // =========================================================

  const handleRefresh = () => {
    setMenuOpen(false);

    window.location.reload();
  };

  // =========================================================
  // FULLSCREEN
  // =========================================================

  const handleFullscreen = async () => {
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen();

        setIsFullscreen(true);
      } else {
        await document.exitFullscreen();

        setIsFullscreen(false);
      }
    } catch (error) {
      console.error(
        "Fullscreen failed:",
        error
      );
    }

    setMenuOpen(false);
  };

  // =========================================================
  // TRACK FULLSCREEN STATE
  // =========================================================

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(
        Boolean(document.fullscreenElement)
      );
    };

    document.addEventListener(
      "fullscreenchange",
      handleFullscreenChange
    );

    return () => {
      document.removeEventListener(
        "fullscreenchange",
        handleFullscreenChange
      );
    };
  }, []);

  // =========================================================
  // UI
  // =========================================================

  return (
    <div className="flex h-full flex-col p-6 overflow-hidden">
      <div className="mb-6 flex flex-col gap-4">

        {/* =====================================================
            BREADCRUMB
        ===================================================== */}

        <div className="flex items-center gap-2 text-sm text-[#5E6C84]">
          <span>Projects</span>

          <ChevronRight className="h-4 w-4" />

          <span>Platform Services</span>

          <ChevronRight className="h-4 w-4" />

          <span>Kanban Board</span>
        </div>

        {/* =====================================================
            TITLE + ACTION BUTTONS
        ===================================================== */}

        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-[#172B4D]">
            Kanban Board
          </h1>

          <div className="flex items-center gap-2">

            {/* =================================================
                SHARE BUTTON
            ================================================= */}

            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={handleShare}
              title="Share board"
              aria-label="Share board"
            >
              <Share2 className="h-4 w-4" />
            </Button>

            {/* =================================================
                THREE DOT MENU
            ================================================= */}

            <div
              ref={menuRef}
              className="relative"
            >
              <Button
                type="button"
                variant="ghost"
                size="icon"
                onClick={() =>
                  setMenuOpen(
                    (previous) =>
                      !previous
                  )
                }
                title="More options"
                aria-label="More options"
              >
                <MoreHorizontal className="h-4 w-4" />
              </Button>

              {/* =================================================
                  DROPDOWN MENU
              ================================================= */}

              {menuOpen && (
                <div
                  className="
                    absolute
                    right-0
                    top-10
                    z-50
                    w-48
                    rounded-md
                    border
                    bg-white
                    p-1
                    shadow-lg
                  "
                >

                  {/* REFRESH */}

                  <button
                    type="button"
                    onClick={
                      handleRefresh
                    }
                    className="
                      flex
                      w-full
                      items-center
                      gap-2
                      rounded-sm
                      px-3
                      py-2
                      text-sm
                      text-[#172B4D]
                      hover:bg-gray-100
                    "
                  >
                    <RefreshCw className="h-4 w-4" />

                    <span>
                      Refresh Board
                    </span>
                  </button>

                  {/* FULLSCREEN */}

                  <button
                    type="button"
                    onClick={
                      handleFullscreen
                    }
                    className="
                      flex
                      w-full
                      items-center
                      gap-2
                      rounded-sm
                      px-3
                      py-2
                      text-sm
                      text-[#172B4D]
                      hover:bg-gray-100
                    "
                  >
                    {isFullscreen ? (
                      <Minimize className="h-4 w-4" />
                    ) : (
                      <Maximize className="h-4 w-4" />
                    )}

                    <span>
                      {isFullscreen
                        ? "Exit Fullscreen"
                        : "Fullscreen"}
                    </span>
                  </button>

                </div>
              )}
            </div>
          </div>
        </div>

        {/* =====================================================
            AVATARS + FILTERS
        ===================================================== */}

        <div className="flex items-center gap-4">
          <div className="flex -space-x-2">
            {[1, 2, 3, 4].map(
              (i) => (
                <Avatar
                  key={i}
                  className="h-8 w-8 border-2 border-white rounded-full"
                >
                  <AvatarImage
                    src={`https://i.pravatar.cc/150?u=${i}`}
                  />

                  <AvatarFallback>
                    U{i}
                  </AvatarFallback>
                </Avatar>
              )
            )}
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-8 rounded-full border-dashed bg-transparent"
          >
            Only My Issues
          </Button>

          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-8 rounded-full border-dashed bg-transparent"
          >
            Recently Updated
          </Button>
        </div>
      </div>

      {/* =======================================================
          KANBAN BOARD
      ======================================================= */}

      <div className="flex-1 overflow-x-auto min-h-0">
        <Suspense
          fallback={
            <div>
              Loading board...
            </div>
          }
        >
          <KanbanBoard />
        </Suspense>
      </div>
    </div>
  );
}