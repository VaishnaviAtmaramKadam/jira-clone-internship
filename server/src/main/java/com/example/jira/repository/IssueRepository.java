package com.example.jira.repository;

import com.example.jira.model.Issue;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IssueRepository extends MongoRepository<Issue, ObjectId> {

    // Get all issues of a project
    List<Issue> findByProjectId(String projectId);

    // Get all subtasks of a parent task
    List<Issue> findByParentTaskId(String parentTaskId);

    // Get all issues which depend on a particular task
    List<Issue> findByDependencyIdsContaining(String issueId);
}