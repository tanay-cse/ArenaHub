package com.arenahub.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findByIsActiveTrueOrderByPointsCostAsc();

    List<Reward> findAllByOrderByCreatedAtDesc();

    Optional<Reward> findByName(String name);
}
