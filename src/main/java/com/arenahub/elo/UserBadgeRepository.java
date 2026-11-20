package com.arenahub.elo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

    @Query("SELECT COUNT(ub) > 0 FROM UserBadge ub WHERE ub.user.id = :userId AND ub.badge.code = :badgeCode")
    boolean existsByUserIdAndBadgeCode(@Param("userId") Long userId, @Param("badgeCode") String badgeCode);

    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);

    List<UserBadge> findByUserIdIn(Collection<Long> userIds);
}
