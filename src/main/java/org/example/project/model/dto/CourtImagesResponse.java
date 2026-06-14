package org.example.project.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourtImagesResponse {
    private Long courtId;
    private List<String> imageUrls;
}

