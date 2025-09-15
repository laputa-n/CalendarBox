package com.calendarbox.backend.attachment.service;

import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.attachment.repository.AttachmentRepository;
import com.calendarbox.backend.global.infra.storage.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final StorageClient storage;

    public void deleteAttachment(Long userId, Long attachmentId) {
        Attachment a = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));

        String originalKey = a.getObjectKey();
        String thumbKey = storage.toThumbKey(originalKey);

        try {
            storage.deleteQuietly(originalKey);
            storage.deleteQuietly(thumbKey);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Storage delete failed. Try again later.", ex);
        }

        attachmentRepository.delete(a);
    }
}
