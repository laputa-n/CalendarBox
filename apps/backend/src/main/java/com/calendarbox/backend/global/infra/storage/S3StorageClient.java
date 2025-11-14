package com.calendarbox.backend.global.infra.storage;

import com.calendarbox.backend.global.config.props.AppS3Props;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

@Service
public class S3StorageClient implements StorageClient {
    private final S3Client s3;
    private final S3Presigner presigner;
    private final AppS3Props p;

    private static final long MAX_OCR_BYTES = 10 * 1024 * 1024; // 10MB 가드(필요시 조정)

    public S3StorageClient(S3Client s3, S3Presigner presigner, AppS3Props p) {
        this.s3 = s3; this.presigner = presigner; this.p = p;
    }

    @Override
    public String presignPut(String key, String contentType, long size) {
        var put = PutObjectRequest.builder()
                .bucket(p.bucket()).key(key).contentType(contentType).contentLength(size)
                .build();
        var pre = presigner.presignPutObject(b->b
                .signatureDuration(Duration.ofMinutes(p.presignMinutes()))
                .putObjectRequest(put));
        return pre.url().toString();
    }

    @Override
    public String presignGet(String key, String downloadName, boolean inline) {
        var get = GetObjectRequest.builder()
                .bucket(p.bucket()).key(key)
                .responseContentDisposition((inline?"inline":"attachment")
                        + "; filename=\"" + downloadName.replace("\"","") + "\"")
                .build();
        var pre = presigner.presignGetObject(b->b
                .signatureDuration(Duration.ofMinutes(p.presignMinutes()))
                .getObjectRequest(get));
        return pre.url().toString();
    }

    @Override
    public void assertExists(String key) {
        s3.headObject(HeadObjectRequest.builder().bucket(p.bucket()).key(key).build());
    }

    @Override
    public String toThumbKey(String originalKey) {
        int i = originalKey.lastIndexOf('/');
        return (i<0) ? p.thumbPrefix()+"/"+originalKey
                : originalKey.substring(0,i)+"/"+p.thumbPrefix()+originalKey.substring(i);
    }

    @Override
    public void deleteQuietly(String key) {
        try {
            s3.deleteObject(b -> b.bucket(p.bucket()).key(key));
        } catch (NoSuchKeyException e) {
            // 이미 없음 → 무시
        } catch (S3Exception e) {
            // 네트워크/권한 등 중요한 오류는 다시 던져도 됨
            // 심플하게는 런타임으로 감싸서 상위에서 5xx 처리
            throw e;
        }
    }

    @Override
    public byte[] getObjectBytes(String key) {
        try {
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(p.bucket())
                    .key(key)
                    .build();

            // 바이트+메타 함께 (contentLength 확인용)
            ResponseBytes<GetObjectResponse> rb = s3.getObjectAsBytes(req);
            long contentLen = rb.response().contentLength() == null ? -1L : rb.response().contentLength();

            // 사이즈 가드
            if (contentLen > 0 && contentLen > MAX_OCR_BYTES) {
                throw new IllegalStateException("S3 object too large for OCR: key=" + key + ", size=" + contentLen);
            }

            byte[] bytes = rb.asByteArray();
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("S3 object empty: key=" + key);
            }
            return bytes;

        } catch (NoSuchKeyException e) {
            // 키가 아예 없는 경우
            throw new IllegalStateException("S3 object not found: key=" + key, e);
        } catch (S3Exception e) {
            // S3 서버 측 에러(권한, 403/404/5xx 등)
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "S3Exception";
            throw new IllegalStateException("S3 error fetching key=" + key + " (" + code + ")", e);
        } catch (SdkClientException e) {
            // 네트워크/타임아웃/클라이언트 구성 문제
            throw new IllegalStateException("S3 client error fetching key=" + key + ": " + e.getMessage(), e);
        }
    }}

