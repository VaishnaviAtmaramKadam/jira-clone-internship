"use client";

import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
  Trash2,
  Pencil,
  Clock,
  Plus,
  X,
  Paperclip,
  Download,
  FileText,
} from "lucide-react";
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/components/ui/avatar";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";

import axiosInstance from "@/lib/Axiosinstance";
import { useAuth } from "@/lib/AuthContext";

/* =========================================================
   TYPES
========================================================= */

type User = {
  id: string;
  name: string;
  email: string;
  role?: string;
  group?: string;
  avatar?: string;
};

type WorkLog = {
  id: string;
  issueId: string;
  userId: string;
  projectId?: string;
  sprintId?: string;
  workDate: string;
  durationMinutes: number;
  description: string;
  createdAt?: string;
  updatedAt?: string;
};

type Attachment = {
  id: string;
  issueId: string;
  projectId: string;
  uploadedBy: string;
  originalFileName: string;
  storedFileName?: string;
  fileType: string;
  fileSize: number;
  filePath?: string;
  createdAt?: string;
};

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
  dueDate?: string;
  createdAt?: string;
  updatedAt?: string;
};

type IssueModelProps = {
  isOpen: boolean;
  onClose: () => void;
  issue: Issue;
  onIssueUpdated?: () => void;
};

/* =========================================================
   COMPONENT
========================================================= */

export default function IssueModel({
  isOpen,
  onClose,
  issue,
  onIssueUpdated,
}: IssueModelProps) {
  const { user } = useAuth();

  const [localIssue, setLocalIssue] =
    useState<Issue | null>(issue ?? null);

  const [users, setUsers] =
    useState<User[]>([]);

  const [assignee, setAssignee] =
    useState<User | null>(null);

  const [selectedAssigneeId, setSelectedAssigneeId] =
    useState<string>(issue?.assigneeId || "");

  const [commentText, setCommentText] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  /* =========================================================
     WORK LOG STATE
  ========================================================= */

  const [workLogs, setWorkLogs] =
    useState<WorkLog[]>([]);

  const [totalMinutes, setTotalMinutes] =
    useState(0);

  const [showWorkLogForm, setShowWorkLogForm] =
    useState(false);

  const [editingWorkLogId, setEditingWorkLogId] =
    useState<string | null>(null);

  const [workDate, setWorkDate] =
    useState("");

  const [durationMinutes, setDurationMinutes] =
    useState("");

  const [workDescription, setWorkDescription] =
    useState("");

  /* =========================================================
     ATTACHMENT STATE
  ========================================================= */

  const [attachments, setAttachments] =
    useState<Attachment[]>([]);

  const [selectedFile, setSelectedFile] =
    useState<File | null>(null);

  const [uploadingFile, setUploadingFile] =
    useState(false);

  const ALLOWED_FILE_TYPES = [
    "pdf",
    "png",
    "jpg",
    "jpeg",
    "docx",
  ];

  const MAX_FILE_SIZE =
    10 * 1024 * 1024;

  /* =========================================================
     ISSUE SYNC
  ========================================================= */

  useEffect(() => {
    setLocalIssue(issue ?? null);

    setSelectedAssigneeId(
      issue?.assigneeId || ""
    );

    setCommentText("");
    setAssignee(null);
    setSelectedFile(null);
  }, [issue]);

  /* =========================================================
     FETCH USERS
  ========================================================= */

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const fetchUsers = async () => {
      try {
        const response =
          await axiosInstance.get("/api/users");

        if (Array.isArray(response.data)) {
          setUsers(response.data);
        } else {
          setUsers([]);
        }
      } catch (error) {
        console.error(
          "Failed to load users:",
          error
        );

        setUsers([]);
      }
    };

    fetchUsers();
  }, [isOpen]);

  /* =========================================================
     FETCH ASSIGNEE
  ========================================================= */

  useEffect(() => {
    if (
      !isOpen ||
      !localIssue?.assigneeId
    ) {
      setAssignee(null);
      return;
    }

    const fetchAssignee = async () => {
      try {
        const response =
          await axiosInstance.get(
            `/api/users/${localIssue.assigneeId}`
          );

        setAssignee(
          response.data || null
        );
      } catch (error) {
        console.error(
          "Failed to fetch assignee:",
          error
        );

        setAssignee(null);
      }
    };

    fetchAssignee();
  }, [
    isOpen,
    localIssue?.assigneeId,
  ]);

  /* =========================================================
     FETCH WORK LOGS
  ========================================================= */

  const fetchWorkLogs = async () => {
    if (!localIssue?.id) {
      setWorkLogs([]);
      setTotalMinutes(0);
      return;
    }

    try {
      const logsResponse =
        await axiosInstance.get(
          `/api/worklogs/issue/${localIssue.id}`
        );

      if (
        Array.isArray(
          logsResponse.data
        )
      ) {
        setWorkLogs(
          logsResponse.data
        );
      } else {
        setWorkLogs([]);
      }

      const totalResponse =
        await axiosInstance.get(
          `/api/worklogs/issue/${localIssue.id}/total`
        );

      setTotalMinutes(
        Number(
          totalResponse.data?.totalMinutes || 0
        )
      );
    } catch (error) {
      console.error(
        "Failed to fetch work logs:",
        error
      );

      setWorkLogs([]);
      setTotalMinutes(0);
    }
  };

  /* =========================================================
     WORK LOG EFFECT
  ========================================================= */

  useEffect(() => {
    if (
      !isOpen ||
      !localIssue?.id
    ) {
      setWorkLogs([]);
      setTotalMinutes(0);
      return;
    }

    fetchWorkLogs();
  }, [
    isOpen,
    localIssue?.id,
  ]);

  /* =========================================================
     FETCH ATTACHMENTS
  ========================================================= */

  const fetchAttachments = async () => {
    if (!localIssue?.id) {
      setAttachments([]);
      return;
    }

    if (!user?.id) {
      setAttachments([]);
      return;
    }

    console.log(
      "Attachment issue ID:",
      localIssue.id
    );

    console.log(
      "Current user ID:",
      user.id
    );

    try {
      const response =
        await axiosInstance.get(
          `/api/attachments/issue/${localIssue.id}`,
          {
            params: {
              userId: user.id,
            },
          }
        );

      console.log(
        "Attachments API response:",
        response.data
      );

      if (
        Array.isArray(response.data)
      ) {
        setAttachments(
          response.data
        );

        console.log(
          "Attachment count from API:",
          response.data.length
        );
      } else {
        setAttachments([]);
      }
    } catch (error: any) {
      console.error(
        "Failed to fetch attachments:",
        error
      );

      console.error(
        "Attachment fetch response:",
        error?.response?.data
      );

      setAttachments([]);
    }
  };

  /* =========================================================
     ONLY ONE ATTACHMENT EFFECT
  ========================================================= */

  useEffect(() => {
    if (
      !isOpen ||
      !localIssue?.id ||
      !user?.id
    ) {
      setAttachments([]);
      return;
    }

    fetchAttachments();
  }, [
    isOpen,
    localIssue?.id,
    user?.id,
  ]);

  /* =========================================================
     FORMAT DURATION
  ========================================================= */

  const formatDuration = (
    minutes: number
  ) => {
    const hours =
      Math.floor(minutes / 60);

    const mins =
      minutes % 60;

    if (
      hours > 0 &&
      mins > 0
    ) {
      return `${hours} hr ${mins} min`;
    }

    if (hours > 0) {
      return `${hours} hr`;
    }

    return `${mins} min`;
  };

  /* =========================================================
     FORMAT FILE SIZE
  ========================================================= */

  const formatFileSize = (
    bytes: number
  ) => {
    if (bytes < 1024) {
      return `${bytes} B`;
    }

    if (bytes < 1024 * 1024) {
      return `${(
        bytes / 1024
      ).toFixed(1)} KB`;
    }

    return `${(
      bytes /
      (1024 * 1024)
    ).toFixed(2)} MB`;
  };

  /* =========================================================
     WORK LOG PERMISSION
  ========================================================= */

  const canModifyWorkLog = () => {
    if (!user || !localIssue) {
      return false;
    }

    const userId =
      String(user.id || "").trim();

    const assigneeId =
      String(
        localIssue.assigneeId || ""
      ).trim();

    const role =
      String(user.role || "")
        .trim()
        .toUpperCase()
        .replace(/_/g, " ");

    const isAssignee =
      userId !== "" &&
      assigneeId !== "" &&
      userId === assigneeId;

    const isProjectManager =
      role === "PROJECT MANAGER";

    return (
      isAssignee ||
      isProjectManager
    );
  };

  /* =========================================================
     ADD WORK LOG
  ========================================================= */

  const openAddWorkLog = () => {
    if (!localIssue?.id) {
      alert("Issue not found.");
      return;
    }

    const today =
      new Date()
        .toISOString()
        .split("T")[0];

    setEditingWorkLogId(null);
    setWorkDate(today);
    setDurationMinutes("");
    setWorkDescription("");
    setShowWorkLogForm(true);
  };

  /* =========================================================
     EDIT WORK LOG
  ========================================================= */

  const openEditWorkLog = (
    log: WorkLog
  ) => {
    if (!canModifyWorkLog()) {
      alert(
        "Only the task assignee or Project Manager can modify time entries."
      );
      return;
    }

    const confirmed =
      window.confirm(
        "Do you want to edit this work log?"
      );

    if (!confirmed) {
      return;
    }

    setEditingWorkLogId(log.id);

    setWorkDate(
      log.workDate
        ? log.workDate.substring(0, 10)
        : ""
    );

    setDurationMinutes(
      String(log.durationMinutes)
    );

    setWorkDescription(
      log.description || ""
    );

    setShowWorkLogForm(true);
  };

  /* =========================================================
     CANCEL WORK LOG
  ========================================================= */

  const cancelWorkLog = () => {
    setShowWorkLogForm(false);
    setEditingWorkLogId(null);
    setWorkDate("");
    setDurationMinutes("");
    setWorkDescription("");
  };

  /* =========================================================
     SAVE WORK LOG
  ========================================================= */

  const saveWorkLog = async () => {
    if (!user) {
      alert("Please login first.");
      return;
    }

    if (!localIssue?.id) {
      alert("Issue not found.");
      return;
    }

    if (
      editingWorkLogId &&
      !canModifyWorkLog()
    ) {
      alert(
        "Only the task assignee or Project Manager can modify time entries."
      );
      return;
    }

    if (!workDate) {
      alert("Work date is required.");
      return;
    }

    const duration =
      Number(durationMinutes);

    if (
      !duration ||
      duration <= 0
    ) {
      alert(
        "Duration must be greater than 0."
      );
      return;
    }

    if (!workDescription.trim()) {
      alert(
        "Work description is required."
      );
      return;
    }

    const today =
      new Date()
        .toISOString()
        .split("T")[0];

    if (workDate > today) {
      alert(
        "Work date cannot be in the future."
      );
      return;
    }

    try {
      setLoading(true);

      if (editingWorkLogId) {
        await axiosInstance.put(
          `/api/worklogs/${editingWorkLogId}`,
          {
            userId: user.id,
            durationMinutes: duration,
            workDate,
            description:
              workDescription.trim(),
          }
        );

        alert(
          "Work log updated successfully."
        );
      } else {
        await axiosInstance.post(
          "/api/worklogs",
          {
            issueId: localIssue.id,
            userId: user.id,
            workDate,
            durationMinutes: duration,
            description:
              workDescription.trim(),
          }
        );

        alert(
          "Work log added successfully."
        );
      }

      cancelWorkLog();

      await fetchWorkLogs();

    } catch (error: any) {
      console.error(
        "Failed to save work log:",
        error
      );

      alert(
        String(
          error?.response?.data ||
            "Failed to save work log."
        )
      );

    } finally {
      setLoading(false);
    }
  };

  /* =========================================================
     DELETE WORK LOG
  ========================================================= */

  const deleteWorkLog = async (
    log: WorkLog
  ) => {
    if (!user) {
      alert("Please login first.");
      return;
    }

    if (!canModifyWorkLog()) {
      alert(
        "Only the task assignee or Project Manager can modify time entries."
      );
      return;
    }

    const confirmed =
      window.confirm(
        "Are you sure you want to delete this work log?"
      );

    if (!confirmed) {
      return;
    }

    try {
      setLoading(true);

      await axiosInstance.delete(
        `/api/worklogs/${log.id}`,
        {
          params: {
            userId: user.id,
          },
        }
      );

      alert(
        "Work log deleted successfully."
      );

      await fetchWorkLogs();

    } catch (error: any) {
      console.error(
        "Failed to delete work log:",
        error
      );

      alert(
        String(
          error?.response?.data ||
            "Failed to delete work log."
        )
      );

    } finally {
      setLoading(false);
    }
  };

  /* =========================================================
     FILE SELECT
  ========================================================= */

  const handleFileSelect = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file =
      event.target.files?.[0];

    if (!file) {
      setSelectedFile(null);
      return;
    }

    const fileName =
      file.name || "";

    const lastDot =
      fileName.lastIndexOf(".");

    const extension =
      lastDot > 0
        ? fileName
            .substring(lastDot + 1)
            .toLowerCase()
        : "";

    if (
      !ALLOWED_FILE_TYPES.includes(
        extension
      )
    ) {
      alert(
        "Only PDF, PNG, JPG, JPEG and DOCX files are allowed."
      );

      event.target.value = "";
      setSelectedFile(null);
      return;
    }

    if (
      file.size > MAX_FILE_SIZE
    ) {
      alert(
        "File size must not exceed 10 MB."
      );

      event.target.value = "";
      setSelectedFile(null);
      return;
    }

    setSelectedFile(file);
  };

  /* =========================================================
     UPLOAD ATTACHMENT
  ========================================================= */

  const uploadAttachment = async () => {
    if (!user) {
      alert("Please login first.");
      return;
    }

    if (!localIssue?.id) {
      alert("Issue not found.");
      return;
    }

    if (!localIssue.projectId) {
      alert(
        "Project information not available."
      );
      return;
    }

    if (!selectedFile) {
      alert("Please select a file.");
      return;
    }

    try {
      setUploadingFile(true);

      const formData =
        new FormData();

      formData.append(
        "file",
        selectedFile
      );

      formData.append(
        "issueId",
        localIssue.id
      );

      formData.append(
        "projectId",
        localIssue.projectId
      );

      formData.append(
        "uploadedBy",
        String(user.id)
      );

      await axiosInstance.post(
        "/api/attachments/upload",
        formData,
        {
          headers: {
            "Content-Type":
              "multipart/form-data",
          },
        }
      );

      setSelectedFile(null);

      const fileInput =
        document.getElementById(
          "issue-attachment-input"
        ) as HTMLInputElement | null;

      if (fileInput) {
        fileInput.value = "";
      }

      await fetchAttachments();

      alert(
        "File uploaded successfully."
      );

    } catch (error: any) {
      console.error(
        "Failed to upload attachment:",
        error
      );

      alert(
        String(
          error?.response?.data ||
            "Failed to upload attachment."
        )
      );

    } finally {
      setUploadingFile(false);
    }
  };

  /* =========================================================
     DOWNLOAD ATTACHMENT
  ========================================================= */

  const downloadAttachment = async (
    attachment: Attachment
  ) => {
    if (!user) {
      alert("Please login first.");
      return;
    }

    try {
      const response =
        await axiosInstance.get(
          `/api/attachments/download/${attachment.id}`,
          {
            params: {
              userId: user.id,
            },
            responseType: "blob",
          }
        );

      const blob =
        new Blob(
          [response.data],
          {
            type:
              response.headers[
                "content-type"
              ] ||
              "application/octet-stream",
          }
        );

      const url =
        window.URL.createObjectURL(
          blob
        );

      const link =
        document.createElement("a");

      link.href = url;

      link.download =
        attachment.originalFileName ||
        "attachment";

      document.body.appendChild(
        link
      );

      link.click();

      link.remove();

      window.URL.revokeObjectURL(
        url
      );

    } catch (error) {
      console.error(
        "Failed to download attachment:",
        error
      );

      alert(
        "Failed to download attachment."
      );
    }
  };

  /* =========================================================
     SAVE ASSIGNEE
  ========================================================= */

  const saveAssignee = async () => {
    if (!localIssue?.id) {
      alert("Issue not found.");
      return;
    }

    try {
      setLoading(true);

      const response =
        await axiosInstance.put(
          `/api/issues/${localIssue.id}`,
          {
            title:
              localIssue.title,

            description:
              localIssue.description,

            type:
              localIssue.type,

            status:
              localIssue.status,

            priority:
              localIssue.priority,

            projectId:
              localIssue.projectId,

            reporterId:
              localIssue.reporterId,

            assigneeId:
              selectedAssigneeId || null,

            parentTaskId:
              localIssue.parentTaskId ?? null,

            sprintId:
              localIssue.sprintId ?? null,

            dependencyIds:
              localIssue.dependencyIds ?? [],

            order:
              localIssue.order ?? 0,

            comments:
              localIssue.comments ?? [],

            dueDate:
              localIssue.dueDate,
          }
        );

      setLocalIssue(
        response.data
      );

      setSelectedAssigneeId(
        response.data?.assigneeId || ""
      );

      onIssueUpdated?.();

      alert(
        "Assignee updated successfully."
      );

    } catch (error: any) {
      console.error(
        "Failed to update assignee:",
        error
      );

      alert(
        String(
          error?.response?.data ||
            "Failed to update assignee."
        )
      );

    } finally {
      setLoading(false);
    }
  };

  /* =========================================================
     ADD COMMENT
  ========================================================= */

  const addComment = async () => {
    if (!localIssue?.id) {
      alert("Issue not found.");
      return;
    }

    if (!commentText.trim()) {
      return;
    }

    try {
      setLoading(true);

      const updatedComments = [
        ...(localIssue.comments ?? []),
        commentText.trim(),
      ];

      const response =
        await axiosInstance.put(
          `/api/issues/${localIssue.id}`,
          {
            title:
              localIssue.title,

            description:
              localIssue.description,

            type:
              localIssue.type,

            status:
              localIssue.status,

            priority:
              localIssue.priority,

            projectId:
              localIssue.projectId,

            reporterId:
              localIssue.reporterId,

            assigneeId:
              localIssue.assigneeId,

            parentTaskId:
              localIssue.parentTaskId ?? null,

            sprintId:
              localIssue.sprintId ?? null,

            dependencyIds:
              localIssue.dependencyIds ?? [],

            order:
              localIssue.order ?? 0,

            comments:
              updatedComments,

            dueDate:
              localIssue.dueDate,
          }
        );

      setLocalIssue(
        response.data
      );

      setCommentText("");

      onIssueUpdated?.();

    } catch (error: any) {
      console.error(
        "Failed to add comment:",
        error
      );

      alert(
        String(
          error?.response?.data ||
            "Failed to add comment."
        )
      );

    } finally {
      setLoading(false);
    }
  };

  /* =========================================================
     DELETE ISSUE
  ========================================================= */

  const deleteIssue = async () => {
    if (!localIssue?.id) {
      alert("Issue not found.");
      return;
    }

    const confirmed =
      window.confirm(
        `Are you sure you want to delete "${localIssue.title}"?`
      );

    if (!confirmed) {
      return;
    }

    try {
      setLoading(true);

      await axiosInstance.delete(
        `/api/issues/${localIssue.id}`
      );

      alert(
        "Issue and its attachments deleted successfully."
      );

      onClose();

      onIssueUpdated?.();

    } catch (error: any) {
      console.error(
        "Failed to delete issue:",
        error
      );

      alert(
        String(
          error?.response?.data ||
            "Failed to delete issue."
        )
      );

    } finally {
      setLoading(false);
    }
  };

  /* =========================================================
     CLOSE
  ========================================================= */

  const handleClose = () => {
    cancelWorkLog();
    setCommentText("");
    setSelectedFile(null);
    onClose();
  };

  /* =========================================================
     RENDER
  ========================================================= */

  if (!localIssue) {
    return null;
  }

  return (
    <Dialog
      open={isOpen}
      onOpenChange={(open) => {
        if (!open) {
          handleClose();
        }
      }}
    >
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">

        <DialogHeader>

          <div className="flex items-center justify-between gap-4">

            <DialogTitle>
              {localIssue.title}
            </DialogTitle>

            <Button
              type="button"
              variant="destructive"
              size="sm"
              onClick={deleteIssue}
              disabled={loading}
            >
              <Trash2 className="h-4 w-4 mr-1" />
              Delete Issue
            </Button>

          </div>

        </DialogHeader>

        <div className="space-y-6">

          {/* =================================================
              DESCRIPTION
          ================================================= */}

          <div>

            <h3 className="text-sm font-semibold mb-2">
              Description
            </h3>

            <p className="text-sm text-muted-foreground">
              {localIssue.description ||
                "No description"}
            </p>

          </div>

          {/* =================================================
              ASSIGNEE
          ================================================= */}

          <div className="border rounded-lg p-4">

            <h3 className="text-sm font-semibold mb-3">
              Assignee
            </h3>

            <div className="flex gap-3 items-center">

              <select
                value={
                  selectedAssigneeId
                }
                onChange={(event) =>
                  setSelectedAssigneeId(
                    event.target.value
                  )
                }
                className="border rounded-md px-3 py-2 text-sm flex-1"
              >

                <option value="">
                  Unassigned
                </option>

                {users.map(
                  (currentUser) => (
                    <option
                      key={
                        currentUser.id
                      }
                      value={
                        currentUser.id
                      }
                    >
                      {currentUser.name}
                    </option>
                  )
                )}

              </select>

              <Button
                type="button"
                onClick={
                  saveAssignee
                }
                disabled={loading}
              >
                Save Assignee
              </Button>

            </div>

            {assignee && (
              <div className="flex items-center gap-2 mt-3">

                <Avatar className="h-8 w-8">

                  <AvatarImage
                    src={
                      assignee.avatar ||
                      ""
                    }
                  />

                  <AvatarFallback>
                    {assignee.name
                      ?.charAt(0)
                      ?.toUpperCase()}
                  </AvatarFallback>

                </Avatar>

                <div>

                  <p className="text-sm font-medium">
                    {assignee.name}
                  </p>

                  <p className="text-xs text-muted-foreground">
                    {assignee.email}
                  </p>

                </div>

              </div>
            )}

          </div>

          {/* =================================================
              ATTACHMENTS
          ================================================= */}

          <div className="border rounded-lg p-4">

            <div className="flex items-center justify-between mb-4">

              <div>

                <h3 className="text-sm font-semibold flex items-center gap-2">

                  <Paperclip className="h-4 w-4" />

                  Attachments

                </h3>

                <p className="text-xs text-muted-foreground mt-1">
                  PDF, PNG, JPG, JPEG and DOCX • Maximum 10 MB
                </p>

              </div>

            </div>

            {/* =================================================
                DEBUG COUNT
            ================================================= */}

            <p className="text-sm text-muted-foreground mb-3">
              Attachment count:{" "}
              <span className="font-semibold">
                {attachments.length}
              </span>
            </p>

            {/* =================================================
                UPLOAD
            ================================================= */}

            <div className="border rounded-lg p-4">

              <div className="flex flex-col md:flex-row gap-3">

                <input
                  id="issue-attachment-input"
                  type="file"
                  accept=".pdf,.png,.jpg,.jpeg,.docx"
                  onChange={
                    handleFileSelect
                  }
                  disabled={
                    uploadingFile
                  }
                  className="block w-full text-sm border rounded-md p-2"
                />

                <Button
                  type="button"
                  onClick={
                    uploadAttachment
                  }
                  disabled={
                    uploadingFile ||
                    !selectedFile
                  }
                >

                  <Paperclip className="h-4 w-4 mr-1" />

                  {uploadingFile
                    ? "Uploading..."
                    : "Upload File"}

                </Button>

              </div>

              {selectedFile && (
                <div className="flex items-center justify-between mt-3 bg-muted rounded-md p-3">

                  <div className="flex items-center gap-2 min-w-0">

                    <FileText className="h-4 w-4 shrink-0" />

                    <div className="min-w-0">

                      <p className="text-sm font-medium truncate">
                        {selectedFile.name}
                      </p>

                      <p className="text-xs text-muted-foreground">
                        {formatFileSize(
                          selectedFile.size
                        )}
                      </p>

                    </div>

                  </div>

                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() =>
                      setSelectedFile(
                        null
                      )
                    }
                    disabled={
                      uploadingFile
                    }
                  >
                    <X className="h-4 w-4" />
                  </Button>

                </div>
              )}

            </div>

            {/* =================================================
                ATTACHMENT LIST
            ================================================= */}

            <div className="mt-4">

              {attachments.length === 0 ? (

                <p className="text-sm text-muted-foreground">
                  No attachments yet.
                </p>

              ) : (

                <div className="space-y-2">

                  {attachments.map(
                    (attachment) => (

                      <div
                        key={
                          attachment.id
                        }
                        className="border rounded-lg p-3 flex items-center justify-between gap-3"
                      >

                        <div className="flex items-center gap-3 min-w-0">

                          <FileText className="h-5 w-5 shrink-0" />

                          <div className="min-w-0">

                            <p className="text-sm font-medium truncate">
                              {
                                attachment.originalFileName
                              }
                            </p>

                            <div className="flex items-center gap-2">

                              <Badge variant="secondary">
                                {attachment.fileType?.toUpperCase()}
                              </Badge>

                              <span className="text-xs text-muted-foreground">
                                {formatFileSize(
                                  attachment.fileSize
                                )}
                              </span>

                            </div>

                          </div>

                        </div>

                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            downloadAttachment(
                              attachment
                            )
                          }
                        >

                          <Download className="h-4 w-4 mr-1" />

                          Download

                        </Button>

                      </div>

                    )
                  )}

                </div>

              )}

            </div>

          </div>

          {/* =================================================
              TIME TRACKING
          ================================================= */}

          <div className="border rounded-lg p-4">

            <div className="flex items-center justify-between mb-4">

              <div>

                <h3 className="text-sm font-semibold flex items-center gap-2">

                  <Clock className="h-4 w-4" />

                  Time Tracking

                </h3>

                <p className="text-sm text-muted-foreground mt-1">

                  Total logged time:{" "}

                  <span className="font-medium text-foreground">

                    {formatDuration(
                      totalMinutes
                    )}

                  </span>

                </p>

              </div>

              <Button
                type="button"
                size="sm"
                onClick={
                  openAddWorkLog
                }
                disabled={loading}
              >

                <Plus className="h-4 w-4 mr-1" />

                Log Work

              </Button>

            </div>

            {/* WORK LOG FORM */}

            {showWorkLogForm && (
              <div className="border rounded-lg p-4 mb-4">

                <div className="flex items-center justify-between mb-4">

                  <h4 className="font-medium">

                    {editingWorkLogId
                      ? "Edit Work Log"
                      : "Add Work Log"}

                  </h4>

                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={
                      cancelWorkLog
                    }
                  >
                    <X className="h-4 w-4" />
                  </Button>

                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

                  <div>

                    <label className="text-sm font-medium">
                      Date
                    </label>

                    <input
                      type="date"
                      value={workDate}
                      max={
                        new Date()
                          .toISOString()
                          .split("T")[0]
                      }
                      onChange={(event) =>
                        setWorkDate(
                          event.target.value
                        )
                      }
                      className="border rounded-md px-3 py-2 w-full mt-1"
                    />

                  </div>

                  <div>

                    <label className="text-sm font-medium">
                      Duration (minutes)
                    </label>

                    <input
                      type="number"
                      min="1"
                      value={
                        durationMinutes
                      }
                      onChange={(event) =>
                        setDurationMinutes(
                          event.target.value
                        )
                      }
                      className="border rounded-md px-3 py-2 w-full mt-1"
                    />

                  </div>

                </div>

                <div className="mt-4">

                  <label className="text-sm font-medium">
                    Work Description
                  </label>

                  <Textarea
                    value={
                      workDescription
                    }
                    onChange={(event) =>
                      setWorkDescription(
                        event.target.value
                      )
                    }
                    placeholder="Describe the work completed..."
                    className="mt-1"
                  />

                </div>

                <div className="flex justify-end gap-2 mt-4">

                  <Button
                    type="button"
                    variant="outline"
                    onClick={
                      cancelWorkLog
                    }
                    disabled={loading}
                  >
                    Cancel
                  </Button>

                  <Button
                    type="button"
                    onClick={
                      saveWorkLog
                    }
                    disabled={loading}
                  >
                    {editingWorkLogId
                      ? "Update Work Log"
                      : "Save Work Log"}
                  </Button>

                </div>

              </div>
            )}

            {/* WORK LOG LIST */}

            {workLogs.length === 0 ? (

              <p className="text-sm text-muted-foreground">
                No work logs yet.
              </p>

            ) : (

              <div className="space-y-3">

                {workLogs.map(
                  (log) => (

                    <div
                      key={log.id}
                      className="border rounded-lg p-3"
                    >

                      <div className="flex items-start justify-between gap-3">

                        <div className="flex-1">

                          <div className="flex items-center gap-2 mb-1">

                            <Badge>
                              {formatDuration(
                                log.durationMinutes
                              )}
                            </Badge>

                            <span className="text-xs text-muted-foreground">
                              {log.workDate}
                            </span>

                          </div>

                          <p className="text-sm">
                            {log.description}
                          </p>

                        </div>

                        <div className="flex gap-1">

                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() =>
                              openEditWorkLog(
                                log
                              )
                            }
                            disabled={loading}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>

                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() =>
                              deleteWorkLog(
                                log
                              )
                            }
                            disabled={loading}
                            className="text-red-600 hover:text-red-700"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>

                        </div>

                      </div>

                    </div>

                  )
                )}

              </div>

            )}

          </div>

          {/* =================================================
              COMMENTS
          ================================================= */}

          <div className="border rounded-lg p-4">

            <h3 className="text-sm font-semibold mb-3">
              Comments
            </h3>

            <div className="space-y-2">

              {(localIssue.comments ?? [])
                .map(
                  (
                    comment,
                    index
                  ) => (

                    <div
                      key={index}
                      className="bg-muted rounded-md p-3 text-sm"
                    >
                      {comment}
                    </div>

                  )
                )}

            </div>

            <div className="flex gap-2 mt-4">

              <Textarea
                value={commentText}
                onChange={(event) =>
                  setCommentText(
                    event.target.value
                  )
                }
                placeholder="Add a comment..."
              />

              <Button
                type="button"
                onClick={addComment}
                disabled={
                  loading ||
                  !commentText.trim()
                }
              >
                Add
              </Button>

            </div>

          </div>

        </div>

      </DialogContent>
    </Dialog>
  );
}