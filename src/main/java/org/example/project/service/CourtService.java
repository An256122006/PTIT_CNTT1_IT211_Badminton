package org.example.project.service;

import org.example.project.model.entity.Court;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourtService {
    Court uploadCourtImages(Long courtId, List<MultipartFile> files);
}
