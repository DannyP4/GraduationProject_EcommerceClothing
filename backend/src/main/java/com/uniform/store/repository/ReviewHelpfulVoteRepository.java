package com.uniform.store.repository;

import com.uniform.store.entity.ReviewHelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {

    Optional<ReviewHelpfulVote> findByReviewIdAndUserId(Long reviewId, Long userId);

    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    @Query("SELECT v.review.id FROM ReviewHelpfulVote v WHERE v.review.id IN :reviewIds AND v.user.id = :userId")
    List<Long> findVotedReviewIds(@Param("reviewIds") Collection<Long> reviewIds, @Param("userId") Long userId);
}
