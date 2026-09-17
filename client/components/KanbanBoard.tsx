"use client";

import {
  closestCorners,
  defaultDropAnimationSideEffects,
  DndContext,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from "@dnd-kit/core";

import { useSearchParams } from "next/navigation";
import React, {
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";

import { createPortal } from "react-dom";

import KanbanColumn from "./KanbanColumn";
import KanbanCard from "./KanbanCard";
import IssueModel from "./IssueModel";

import axiosInstance from "@/lib/Axiosinstance";
import { useAuth } from "@/lib/AuthContext";

import {
  connectWebSocket,
  disconnectWebSocket,
} from "@/app/services/websocket";

const STATUS_COLUMNS = [
  {
    id: "TODO",
    title: "To Do",
  },
  {
    id: "IN_PROGRESS",
    title: "In Progress",
  },
  {
    id: "DONE",
    title: "Done",
  },
];

type Issue = {
  id: string;
  key?: string;
  title: string;
  description?: string;
  type?: string;
  status?: string;
  priority?: string;
  projectId?: string;
  reporterId?: string;
  assigneeId?: string;
  parentTaskId?: string | null;
  sprintId?: string | null;
  dependencyIds?: string[];
  order?: number;
  comments?: string[];
  updatedAt?: string;
};

const KanbanBoard = () => {
  const {
    user,
    selectedProject,
  } = useAuth();

  const searchParams = useSearchParams();

  const searchQuery =
    searchParams.get("search")?.toLowerCase() || "";

  const [issues, setIssues] =
    useState<Issue[]>([]);

  const [activeIssue, setActiveIssue] =
    useState<Issue | null>(null);

  const [selectedIssue, setSelectedIssue] =
    useState<Issue | null>(null);

  const [loading, setLoading] =
    useState(false);

  const [isMounted, setIsMounted] =
    useState(false);

  // =========================================================
  // DUPLICATE WEBSOCKET EVENT PREVENTION
  // =========================================================

  const processedEvents =
    useRef<Set<string>>(new Set());

  // =========================================================
  // MOUNT
  // =========================================================

  useEffect(() => {
    setIsMounted(true);
  }, []);

  // =========================================================
  // DRAG SENSORS
  // =========================================================

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5,
      },
    }),
    useSensor(KeyboardSensor)
  );

  // =========================================================
  // FETCH ISSUES
  // =========================================================

  const fetchIssues =
    useCallback(async () => {
      if (!selectedProject?.id) {
        setIssues([]);
        return;
      }

      try {
        setLoading(true);

        const response =
          await axiosInstance.get(
            `/api/issues/project/${selectedProject.id}`
          );

        setIssues(
          Array.isArray(response.data)
            ? response.data
            : []
        );
      } catch (error) {
        console.error(
          "Failed to load issues:",
          error
        );
      } finally {
        setLoading(false);
      }
    }, [selectedProject?.id]);

  // =========================================================
  // INITIAL ISSUE LOAD
  // =========================================================

  useEffect(() => {
    fetchIssues();
  }, [fetchIssues]);

  // =========================================================
  // WEBSOCKET REAL-TIME CONNECTION
  // =========================================================

  useEffect(() => {
    const projectId =
      selectedProject?.id;

    const userId =
      user?.id;

    if (!projectId || !userId) {
      return;
    }

    const handleWebSocketMessage = (
      event: any
    ) => {
      console.log(
        "Received WebSocket event:",
        event
      );

      if (!event?.eventType) {
        return;
      }

      // =====================================================
      // DUPLICATE EVENT PREVENTION
      // =====================================================

      const issueId =
        event?.data?.id || "";

      const updatedAt =
        event?.data?.updatedAt || "";

      const eventKey =
        `${event.eventType}-${issueId}-${updatedAt}`;

      if (
        eventKey !== "--" &&
        processedEvents.current.has(
          eventKey
        )
      ) {
        console.log(
          "Duplicate WebSocket event ignored:",
          eventKey
        );

        return;
      }

      if (eventKey !== "--") {
        processedEvents.current.add(
          eventKey
        );

        setTimeout(() => {
          processedEvents.current.delete(
            eventKey
          );
        }, 10000);
      }

      // =====================================================
      // HANDLE EVENTS
      // =====================================================

      switch (event.eventType) {
        case "ISSUE_CREATED":
        case "ISSUE_UPDATED":
        case "ISSUE_STATUS_CHANGED":
        case "COMMENT_ADDED":
        case "ISSUE_DELETED":

          fetchIssues();

          break;

        default:

          console.log(
            "Unknown WebSocket event:",
            event.eventType
          );
      }
    };

    console.log(
      "Connecting WebSocket for project:",
      projectId,
      "user:",
      userId
    );

    connectWebSocket(
      projectId,
      userId,
      handleWebSocketMessage
    );

    // =======================================================
    // CLEANUP
    // =======================================================

    return () => {
      console.log(
        "Disconnecting WebSocket for project:",
        projectId
      );

      disconnectWebSocket();

      processedEvents.current.clear();
    };
  }, [
    selectedProject?.id,
    user?.id,
    fetchIssues,
  ]);

  // =========================================================
  // DRAG START
  // =========================================================

  const onDragStart = (
    event: DragStartEvent
  ) => {
    const issue = issues.find(
      (item) =>
        String(item.id) ===
        String(event.active.id)
    );

    setActiveIssue(
      issue || null
    );
  };

  // =========================================================
  // DRAG END
  // =========================================================

  const onDragEnd = async (
    event: DragEndEvent
  ) => {
    setActiveIssue(null);

    const {
      active,
      over,
    } = event;

    if (!over) {
      return;
    }

    const issueId =
      String(active.id);

    const overId =
      String(over.id);

    // =======================================================
    // FIND TARGET COLUMN
    // =======================================================

    const targetColumn =
      STATUS_COLUMNS.find(
        (column) =>
          column.id === overId
      );

    // =======================================================
    // FIND TARGET ISSUE
    // =======================================================

    const targetIssue =
      issues.find(
        (issue) =>
          String(issue.id) ===
          overId
      );

    const newStatus =
      targetColumn
        ? targetColumn.id
        : targetIssue?.status;

    if (!newStatus) {
      return;
    }

    // =======================================================
    // FIND CURRENT ISSUE
    // =======================================================

    const issue =
      issues.find(
        (item) =>
          String(item.id) ===
          issueId
      );

    if (!issue) {
      return;
    }

    // =======================================================
    // SAME STATUS
    // =======================================================

    if (
      issue.status ===
      newStatus
    ) {
      return;
    }

    // =======================================================
    // PARENT TASK VALIDATION
    // =======================================================

    if (newStatus === "DONE") {
      const subtasks =
        issues.filter(
          (item) =>
            String(
              item.parentTaskId || ""
            ) ===
            String(issue.id)
        );

      const incompleteSubtasks =
        subtasks.filter(
          (subtask) =>
            subtask.status !==
            "DONE"
        );

      if (
        incompleteSubtasks.length >
        0
      ) {
        alert(
          "Parent task cannot be marked as DONE until all subtasks are completed."
        );

        return;
      }
    }

    // =======================================================
    // DEPENDENCY VALIDATION
    // =======================================================

    if (
      newStatus === "IN_PROGRESS" ||
      newStatus === "DONE"
    ) {
      const dependencyIds =
        issue.dependencyIds || [];

      for (
        const dependencyId of dependencyIds
      ) {
        const dependency =
          issues.find(
            (item) =>
              String(item.id) ===
              String(dependencyId)
          );

        if (
          dependency &&
          dependency.status !==
            "DONE"
        ) {
          alert(
            `Task cannot start because dependency task is not completed: ${dependency.title}`
          );

          return;
        }
      }
    }

    // =======================================================
    // SAVE OLD ISSUE FOR ROLLBACK
    // =======================================================

    const oldIssue: Issue = {
      ...issue,
    };

    // =======================================================
    // OPTIMISTIC UPDATE
    // =======================================================

    const updatedIssue: Issue = {
      ...issue,
      status: newStatus,
      updatedAt:
        new Date().toISOString(),
    };

    setIssues(
      (previousIssues) =>
        previousIssues.map(
          (item) =>
            String(item.id) ===
            issueId
              ? updatedIssue
              : item
        )
    );

    // =======================================================
    // BACKEND UPDATE
    // =======================================================

    try {
      const response =
        await axiosInstance.put(
          `/api/issues/${issueId}`,
          {
            title:
              updatedIssue.title,

            description:
              updatedIssue.description,

            type:
              updatedIssue.type,

            priority:
              updatedIssue.priority,

            status:
              updatedIssue.status,

            projectId:
              updatedIssue.projectId,

            reporterId:
              updatedIssue.reporterId,

            assigneeId:
              updatedIssue.assigneeId,

            parentTaskId:
              updatedIssue.parentTaskId ??
              null,

            sprintId:
              updatedIssue.sprintId ??
              null,

            dependencyIds:
              updatedIssue.dependencyIds ??
              [],

            order:
              updatedIssue.order ??
              0,

            comments:
              updatedIssue.comments ??
              [],

            updatedAt:
              updatedIssue.updatedAt,
          }
        );

      const backendIssue =
        response?.data ||
        updatedIssue;

      // =====================================================
      // UPDATE LOCAL BOARD
      // =====================================================

      setIssues(
        (previousIssues) =>
          previousIssues.map(
            (item) =>
              String(item.id) ===
              issueId
                ? backendIssue
                : item
          )
      );

      // =====================================================
      // UPDATE OPEN MODAL
      // =====================================================

      setSelectedIssue(
        (currentIssue) => {
          if (
            currentIssue &&
            String(
              currentIssue.id
            ) === issueId
          ) {
            return backendIssue;
          }

          return currentIssue;
        }
      );
    } catch (error: any) {
      console.error(
        "Failed to update issue:",
        error
      );

      // =====================================================
      // ROLLBACK
      // =====================================================

      setIssues(
        (previousIssues) =>
          previousIssues.map(
            (item) =>
              String(item.id) ===
              issueId
                ? oldIssue
                : item
          )
      );

      // =====================================================
      // ERROR MESSAGE
      // =====================================================

      let errorMessage =
        "Failed to update issue.";

      if (
        typeof error?.response
          ?.data === "string"
      ) {
        errorMessage =
          error.response.data;
      } else if (
        error?.response?.data
          ?.message
      ) {
        errorMessage =
          error.response.data.message;
      }

      alert(errorMessage);
    }
  };

  // =========================================================
  // ISSUE UPDATED
  // =========================================================

  const handleIssueUpdated =
    async () => {
      await fetchIssues();

      if (
        selectedIssue?.id
      ) {
        try {
          const response =
            await axiosInstance.get(
              `/api/issues/${selectedIssue.id}`
            );

          if (response.data) {
            setSelectedIssue(
              response.data
            );
          }
        } catch (error: any) {
          if (
            error?.response?.status ===
            404
          ) {
            setSelectedIssue(null);
          } else {
            console.error(
              "Failed to refresh selected issue:",
              error
            );
          }
        }
      }
    };

  // =========================================================
  // ISSUE CLICK
  // =========================================================

  const handleIssueClick = (
    issue: Issue
  ) => {
    if (!issue?.id) {
      return;
    }

    setSelectedIssue(issue);
  };

  // =========================================================
  // NO PROJECT
  // =========================================================

  if (!selectedProject) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-[#6B778C]">
        Select a project to view the board
      </div>
    );
  }

  // =========================================================
  // BOARD
  // =========================================================

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={
        closestCorners
      }
      onDragStart={
        onDragStart
      }
      onDragEnd={
        onDragEnd
      }
    >
      {loading ? (
        <div className="flex h-full items-center justify-center text-sm text-[#6B778C]">
          Loading board…
        </div>
      ) : (
        <div className="flex h-full gap-4 pb-4">
          {STATUS_COLUMNS.map(
            (column) => {
              const columnIssues =
                issues
                  .filter(
                    (issue) =>
                      issue.status ===
                      column.id
                  )
                  .filter(
                    (issue) =>
                      issue?.title
                        ?.toLowerCase()
                        .includes(
                          searchQuery
                        ) ||
                      issue?.key
                        ?.toLowerCase()
                        .includes(
                          searchQuery
                        )
                  )
                  .sort(
                    (a, b) =>
                      (a.order ??
                        0) -
                      (b.order ??
                        0)
                  );

              return (
                <KanbanColumn
                  key={column.id}
                  column={column}
                  issues={
                    columnIssues
                  }
                  onIssueClick={
                    handleIssueClick
                  }
                />
              );
            }
          )}
        </div>
      )}

      {/* =====================================================
          ISSUE MODAL
      ===================================================== */}

      {selectedIssue?.id && (
        <IssueModel
          issue={selectedIssue}
          isOpen={true}
          onClose={() =>
            setSelectedIssue(null)
          }
          onIssueUpdated={
            handleIssueUpdated
          }
        />
      )}

      {/* =====================================================
          DRAG OVERLAY
      ===================================================== */}

      {isMounted &&
        !loading &&
        createPortal(
          <DragOverlay
            dropAnimation={{
              sideEffects:
                defaultDropAnimationSideEffects(
                  {
                    styles: {
                      active: {
                        opacity:
                          "0.5",
                      },
                    },
                  }
                ),
            }}
          >
            {activeIssue ? (
              <KanbanCard
                issue={
                  activeIssue
                }
                isOverlay
              />
            ) : null}
          </DragOverlay>,
          document.body
        )}
    </DndContext>
  );
};

export default KanbanBoard;