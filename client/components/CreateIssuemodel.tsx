"use client";

import React, { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "./ui/dialog";
import { AlertCircle } from "lucide-react";
import { Input } from "./ui/input";
import { Button } from "./ui/button";
import { Textarea } from "./ui/textarea";
import { useAuth } from "@/lib/AuthContext";
import axiosInstance from "@/lib/Axiosinstance";

const CreateIssuemodel = ({ isOpen, onClose }: any) => {
  const { user, selectedProject } = useAuth();

  const [isloading, setIsloading] = useState(false);
  const [error, setError] = useState("");
  const [teamMembers, setteamMembers] = useState<any[]>([]);
  const [projectIssues, setProjectIssues] = useState<any[]>([]);

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    type: "TASK",
    priority: "MEDIUM",
    assigneeId: "",
    parentTaskId: "",
    dependencyIds: [] as string[],
    dueDate: "",
  });

  // =========================
  // FETCH TEAM MEMBERS
  // =========================
  useEffect(() => {
    if (!selectedProject?.id || !isOpen) return;

    const fetchMembers = async () => {
      try {
        const res = await axiosInstance.get(
          `/api/projects/${selectedProject.id}`
        );

        setteamMembers(res.data.members || []);
      } catch (error) {
        console.log(error);
      }
    };

    fetchMembers();
  }, [selectedProject?.id, isOpen]);

  // =========================
  // FETCH PROJECT ISSUES
  // =========================
  useEffect(() => {
    if (!selectedProject?.id || !isOpen) return;

    const fetchIssues = async () => {
      try {
        const res = await axiosInstance.get(
          `/api/issues/project/${selectedProject.id}`
        );

        setProjectIssues(res.data || []);
      } catch (error) {
        console.log(error);
      }
    };

    fetchIssues();
  }, [selectedProject?.id, isOpen]);

  // =========================
  // HANDLE NORMAL CHANGE
  // =========================
  const handleChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >
  ) => {
    const { name, value } = e.target;

    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));

    setError("");
  };

  // =========================
  // HANDLE DEPENDENCY CHANGE
  // =========================
  const handleDependencyChange = (
    e: React.ChangeEvent<HTMLSelectElement>
  ) => {
    const selectedOptions = Array.from(
      e.target.selectedOptions
    ).map((option) => option.value);

    setFormData((prevData) => ({
      ...prevData,
      dependencyIds: selectedOptions,
    }));

    setError("");
  };

  // =========================
  // CREATE ISSUE
  // =========================
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!user || !selectedProject) {
      setError("Project and user are required");
      return;
    }

    if (!formData.title.trim()) {
      setError("Issue title is required");
      return;
    }

    // =========================
    // VALIDATE PARENT TASK
    // =========================
    if (
      formData.parentTaskId &&
      !projectIssues.some(
        (issue) => issue.id === formData.parentTaskId
      )
    ) {
      setError("Selected parent task does not exist");
      return;
    }

    // =========================
    // VALIDATE DEPENDENCIES
    // =========================
    const invalidDependency = formData.dependencyIds.some(
      (dependencyId) =>
        !projectIssues.some(
          (issue) => issue.id === dependencyId
        )
    );

    if (invalidDependency) {
      setError("One or more selected dependencies do not exist");
      return;
    }

    // =========================
    // VALIDATE DUE DATE
    // =========================
    if (formData.dueDate) {
      const selectedDueDate = new Date(formData.dueDate);

      if (selectedDueDate.getTime() <= Date.now()) {
        setError("Due date must be in the future");
        return;
      }
    }

    try {
      setIsloading(true);
      setError("");

      await axiosInstance.post("/api/issues", {
        title: formData.title,
        description: formData.description,
        type: formData.type,
        priority: formData.priority,

        status: "TODO",

        projectId: selectedProject.id,
        reporterId: user.id,

        assigneeId: formData.assigneeId || null,

        // =========================
        // SUBTASK
        // =========================
        parentTaskId: formData.parentTaskId || null,

        // Backend will inherit sprint
        // from parent task when applicable.
        sprintId: null,

        // =========================
        // DEPENDENCIES
        // =========================
        dependencyIds: formData.dependencyIds,

        // =========================
        // DUE DATE
        // =========================
        dueDate: formData.dueDate
          ? new Date(formData.dueDate).toISOString()
          : null,

        order: 0,
      });

      // Close modal after successful creation
      onClose();
    } catch (error: any) {
      console.log(error);

      const message =
        error?.response?.data?.message ||
        error?.response?.data ||
        "Failed to create issue";

      setError(
        typeof message === "string"
          ? message
          : "Failed to create issue"
      );
    } finally {
      setIsloading(false);

      setFormData({
        title: "",
        description: "",
        type: "TASK",
        priority: "MEDIUM",
        assigneeId: "",
        parentTaskId: "",
        dependencyIds: [],
        dueDate: "",
      });
    }
  };

  // =========================
  // MAIN TASKS ONLY
  // =========================
  const parentTasks = projectIssues.filter(
    (issue) => !issue.parentTaskId
  );

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Create Issue</DialogTitle>
        </DialogHeader>

        <form
          onSubmit={handleSubmit}
          className="space-y-4"
        >
          {/* ERROR */}
          {error && (
            <div className="flex gap-3 rounded-md bg-red-50 p-3 text-sm text-red-700">
              <AlertCircle className="h-4 w-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {/* TITLE */}
          <div className="space-y-2">
            <label className="text-sm font-semibold text-[#172B4D]">
              Issue Title *
            </label>

            <Input
              type="text"
              name="title"
              placeholder="e.g., Implement user authentication"
              required
              className="h-10 border-[#DFE1E6] focus-visible:ring-[#0052CC]"
              value={formData.title}
              onChange={handleChange}
            />
          </div>

          {/* DESCRIPTION */}
          <div className="space-y-2">
            <label className="text-sm font-semibold text-[#172B4D]">
              Description
            </label>

            <Textarea
              name="description"
              placeholder="Add a description (optional)"
              className="min-h-[100px] border-[#DFE1E6] focus-visible:ring-[#0052CC] resize-none"
              value={formData.description}
              onChange={handleChange}
            />
          </div>

          {/* TYPE / PRIORITY / ASSIGNEE */}
          <div className="grid grid-cols-3 gap-4">

            {/* TYPE */}
            <div className="space-y-2">
              <label className="text-sm font-semibold text-[#172B4D]">
                Type
              </label>

              <select
                name="type"
                className="w-full h-10 rounded border border-[#DFE1E6] bg-white px-3 text-sm text-[#172B4D]"
                value={formData.type}
                onChange={handleChange}
              >
                <option value="TASK">Task</option>
                <option value="BUG">Bug</option>
                <option value="STORY">Story</option>
              </select>
            </div>

            {/* PRIORITY */}
            <div className="space-y-2">
              <label className="text-sm font-semibold text-[#172B4D]">
                Priority
              </label>

              <select
                name="priority"
                className="w-full h-10 rounded border border-[#DFE1E6] bg-white px-3 text-sm text-[#172B4D]"
                value={formData.priority}
                onChange={handleChange}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
              </select>
            </div>

            {/* ASSIGNEE */}
            <div className="space-y-2">
              <label className="text-sm font-semibold text-[#172B4D]">
                Assignee
              </label>

              <select
                name="assigneeId"
                className="w-full h-10 rounded border border-[#DFE1E6] bg-white px-3 text-sm text-[#172B4D]"
                value={formData.assigneeId}
                onChange={handleChange}
              >
                <option value="">
                  Unassigned
                </option>

                {teamMembers.map((member: any) => (
                  <option
                    key={member.id}
                    value={member.id}
                  >
                    {member.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* =========================
              DUE DATE
             ========================= */}
          <div className="space-y-2">
            <label className="text-sm font-semibold text-[#172B4D]">
              Due Date
            </label>

            <Input
              type="datetime-local"
              name="dueDate"
              value={formData.dueDate}
              onChange={handleChange}
              className="h-10 border-[#DFE1E6] focus-visible:ring-[#0052CC]"
            />

            <p className="text-xs text-[#6B778C]">
              Set a deadline for this task. A reminder will be
              generated 24 hours before the due date.
            </p>
          </div>

          {/* =========================
              PARENT TASK
             ========================= */}
          <div className="space-y-2">
            <label className="text-sm font-semibold text-[#172B4D]">
              Parent Task
            </label>

            <select
              name="parentTaskId"
              className="w-full h-10 rounded border border-[#DFE1E6] bg-white px-3 text-sm text-[#172B4D]"
              value={formData.parentTaskId}
              onChange={handleChange}
            >
              <option value="">
                No Parent Task — Main Issue
              </option>

              {parentTasks.map((issue: any) => (
                <option
                  key={issue.id}
                  value={issue.id}
                >
                  {issue.title}
                </option>
              ))}
            </select>

            <p className="text-xs text-[#6B778C]">
              Select a parent task to create this issue
              as a subtask. The subtask will automatically
              inherit the parent&apos;s project and sprint.
            </p>
          </div>

          {/* =========================
              DEPENDENCIES
             ========================= */}
          <div className="space-y-2">
            <label className="text-sm font-semibold text-[#172B4D]">
              Dependencies
            </label>

            <select
              multiple
              name="dependencyIds"
              className="w-full min-h-[110px] rounded border border-[#DFE1E6] bg-white px-3 py-2 text-sm text-[#172B4D]"
              value={formData.dependencyIds}
              onChange={handleDependencyChange}
            >
              {projectIssues.map((issue: any) => (
                <option
                  key={issue.id}
                  value={issue.id}
                >
                  {issue.title}
                </option>
              ))}
            </select>

            <p className="text-xs text-[#6B778C]">
              Select one or more tasks that must be
              completed before this issue can start.
              Hold Ctrl to select multiple tasks.
            </p>
          </div>

          {/* BUTTONS */}
          <div className="flex justify-end gap-2 pt-4 border-t">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isloading}
            >
              Cancel
            </Button>

            <Button
              type="submit"
              className="bg-[#0052CC] text-white hover:bg-[#0747A6]"
              disabled={isloading}
            >
              {isloading
                ? "Creating..."
                : formData.parentTaskId
                  ? "Create Subtask"
                  : "Create Issue"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};

export default CreateIssuemodel;