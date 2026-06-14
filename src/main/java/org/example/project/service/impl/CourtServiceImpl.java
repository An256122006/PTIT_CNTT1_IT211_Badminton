package org.example.project.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.project.exception.HttpNotFoundException;
import org.example.project.exception.HttpUploadImageException;
import org.example.project.model.entity.Court;
import org.example.project.model.entity.CourtImage;
import org.example.project.repository.CourtRepository;
import org.example.project.repository.CourtImgRepository;
import org.example.project.service.CloudinaryService;
import org.example.project.service.CourtService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourtServiceImpl implements CourtService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/jpg");

    private final CourtRepository courtRepository;
    private final CourtImgRepository courtImgRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public Court uploadCourtImages(Long courtId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one image file is required");
        }

        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new HttpNotFoundException("Court not found"));
        List<MultipartFile> validFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            validateImage(file);
            validFiles.add(file);
        }

        if (validFiles.isEmpty()) {
            throw new IllegalArgumentException("At least one non-empty valid image file is required");
        }
        List<String> urls;
        try {
            urls = cloudinaryService.uploadFiles(validFiles);
        } catch (Exception e) {
            throw new HttpUploadImageException("Upload failed: " + e.getMessage());
        }

        List<CourtImage> images = new ArrayList<>();
        for (String url : urls) {
            images.add(CourtImage.builder()
                    .court(court)
                    .imageUrl(url)
                    .build());
        }

        if (!images.isEmpty()) {
            List<CourtImage> saved = courtImgRepository.saveAll(images);
            String lastUrl = saved.get(saved.size() - 1).getImageUrl();
            court.setImageUrl(lastUrl);
        }

        return courtRepository.save(court);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file must not be empty");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file size must not exceed 5MB");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_IMAGE_TYPES.contains(ct)) {
            throw new IllegalArgumentException("Only PNG and JPG images are allowed");
        }
    }
}
