package com.z.c.woodexcess_api.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String upload(MultipartFile file, String folder) throws Exception;

    String getPubliUrl(String key);

    String getPressignUrl(String key, int durationMinutes);

    void delete (String key);

    boolean exists (String key);
}
