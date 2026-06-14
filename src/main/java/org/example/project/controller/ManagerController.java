package org.example.project.controller;

import lombok.RequiredArgsConstructor;
import org.example.project.service.CourtService;
import org.example.project.repository.CourtImgRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.example.project.model.dto.CourtImagesResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/manager")
public class ManagerController {

    private final CourtService courtService;
    private final CourtImgRepository courtImgRepository;

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping(value = "/court/{id}/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCourtImages(
            @PathVariable Long id,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        List<MultipartFile> safeFiles = files == null ? Collections.emptyList() : files;
        if (file != null && !file.isEmpty()) {
            safeFiles = new java.util.ArrayList<>(safeFiles);
            safeFiles.add(file);
        }
        if (safeFiles.isEmpty() || safeFiles.stream().allMatch(MultipartFile::isEmpty)) {
            return ResponseEntity.badRequest().body("No files attached or all files are empty");
        }

        var savedCourt = courtService.uploadCourtImages(id, safeFiles);
        List<String> urls = courtImgRepository.findByCourtId(id).stream()
                .map(org.example.project.model.entity.CourtImage::getImageUrl)
                .collect(Collectors.toList());

        CourtImagesResponse resp = new CourtImagesResponse(savedCourt.getId(), urls);
        return ResponseEntity.ok(resp);
    }
}

