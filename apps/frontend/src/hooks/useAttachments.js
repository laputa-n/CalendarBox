// /src/hooks/useAttachments.js
import { useState } from 'react';
import { ApiService } from '../services/apiService';

export function useAttachments(scheduleId) {
  const [imageQueue, setImageQueue] = useState([]);
  const [fileQueue, setFileQueue] = useState([]);
  const [attachments, setAttachments] = useState({ images: [], files: [] });

  // === 파일 선택 ===
  const handleSelectFiles = (e) => {
    const files = Array.from(e.target.files || []);
    const images = files.filter(f => f.type.startsWith('image/'));
    const others = files.filter(f => !f.type.startsWith('image/'));
    setImageQueue(prev => [...prev, ...images]);
    setFileQueue(prev => [...prev, ...others]);
  };

  // === 업로드 ===
  const uploadFiles = async (id) => {


    const doUpload = async (file) => {
  try {
    const normalizedType =
      file.type?.split(';')[0] || 'application/octet-stream';

    // presign에는 원본 file 대신 type을 보정해서 보내는 게 안전
    const fileForPresign = new File([file], file.name, { type: normalizedType });

    const presign = await ApiService.getPresignedUrl(id, fileForPresign, false);
    const { uploadId, objectKey, presignedUrl } = presign.data;

    await fetch(presignedUrl, {
      method: 'PUT',
      headers: { 'Content-Type': normalizedType },
      body: file,
    });

    await ApiService.completeUpload(uploadId, objectKey, false);
  } catch (err) {
    console.error('파일 업로드 실패:', file.name, err);
  }
};
    // 순차 업로드
    for (const f of [...imageQueue, ...fileQueue]) {
      await doUpload(f);
    }

    // 업로드 후 큐 비우기
    setImageQueue([]);
    setFileQueue([]);
  };

  // === 첨부 조회 ===
  const loadAttachments = async () => {
    if (!scheduleId) return;
    try {
      const [images, files] = await Promise.all([
        ApiService.getImageAttachments(scheduleId),
        ApiService.getFileAttachments(scheduleId),
      ]);
      setAttachments({
        images: images?.data || [],
        files: files?.data || [],
      });
    } catch (err) {
      console.error('첨부 조회 실패:', err);
    }
  };

  // === 삭제 ===
  const handleDelete = async (attachmentId) => {
    if (!window.confirm('삭제하시겠습니까?')) return;
    try {
      await ApiService.deleteAttachment(attachmentId);
      await loadAttachments();
    } catch (err) {
      console.error('첨부 삭제 실패:', err);
    }
  };

  return {
    imageQueue,
    fileQueue,
    attachments,
    handleSelectFiles,
    uploadFiles,
    loadAttachments,
    handleDelete,
  };
}
