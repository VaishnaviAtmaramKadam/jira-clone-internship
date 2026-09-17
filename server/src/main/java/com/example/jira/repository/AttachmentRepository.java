package com.example.jira.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.jira.model.Attachment;

public interface AttachmentRepository
        extends MongoRepository<Attachment, ObjectId> {

    List<Attachment> findByIssueId(String issueId);

    List<Attachment> findByProjectId(String projectId);

    void deleteByIssueId(String issueId);
}
