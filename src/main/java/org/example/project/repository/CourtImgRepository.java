package org.example.project.repository;

import org.example.project.model.entity.CourtImage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtImgRepository extends JpaRepository<CourtImage, Long> {
    List<CourtImage> findByCourtId(Long courtId);
}

