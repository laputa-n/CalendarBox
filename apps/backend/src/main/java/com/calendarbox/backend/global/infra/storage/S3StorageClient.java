package com.calendarbox.backend.global.infra.storage;

import com.calendarbox.backend.global.config.props.AppS3Props;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

@Service
public class S3StorageClient implements StorageClient {
    private final S3Client s3;
    private final S3Presigner presigner;
    private final AppS3Props p;

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
        var resp = s3.getObjectAsBytes(b -> b.bucket(p.bucket()).key(key));
        return resp.asByteArray();
    }
}

