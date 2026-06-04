package com.uniform.store.repository;

import com.uniform.store.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    List<ReviewImage> findByReviewIdOrderBySortOrderAscIdAsc(Long reviewId);

    List<ReviewImage> findByReviewIdInOrderBySortOrderAscIdAsc(Collection<Long> reviewIds);
}
