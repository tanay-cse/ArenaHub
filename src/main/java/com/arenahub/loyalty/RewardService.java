package com.arenahub.loyalty;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RewardService {

    private final RewardRepository rewardRepository;

    public RewardService(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @Transactional(readOnly = true)
    public List<Reward> getActiveRewards() {
        return rewardRepository.findByIsActiveTrueOrderByPointsCostAsc();
    }

    @Transactional(readOnly = true)
    public List<Reward> getAllRewards() {
        return rewardRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Reward> getRewardById(Long id) {
        return rewardRepository.findById(id);
    }

    public Reward saveReward(Reward reward) {
        if (reward.getName() == null || reward.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Reward name is required.");
        }
        reward.setName(reward.getName().trim());

        if (reward.getPointsCost() == null || reward.getPointsCost() <= 0) {
            throw new IllegalArgumentException("Points cost must be greater than 0.");
        }

        if (reward.getStockQuantity() == null || reward.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity must be 0 or greater.");
        }

        if (reward.getIsActive() == null) {
            reward.setIsActive(true);
        }

        Optional<Reward> existingWithName = rewardRepository.findByName(reward.getName());

        if (reward.getId() == null) {
            if (existingWithName.isPresent()) {
                throw new IllegalArgumentException("A reward with the name '" + reward.getName() + "' already exists.");
            }
            return rewardRepository.save(reward);
        } else {
            if (existingWithName.isPresent() && !existingWithName.get().getId().equals(reward.getId())) {
                throw new IllegalArgumentException("Another reward with the name '" + reward.getName() + "' already exists.");
            }
            Reward existing = rewardRepository.findById(reward.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Reward not found with ID: " + reward.getId()));
            existing.setName(reward.getName());
            existing.setDescription(reward.getDescription());
            existing.setPointsCost(reward.getPointsCost());
            existing.setStockQuantity(reward.getStockQuantity());
            existing.setIsActive(reward.getIsActive());
            return rewardRepository.save(existing);
        }
    }

    public void deactivateReward(Long id) {
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found with ID: " + id));
        reward.setIsActive(false);
        rewardRepository.save(reward);
    }

    public void toggleRewardActive(Long id) {
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found with ID: " + id));
        reward.setIsActive(!Boolean.TRUE.equals(reward.getIsActive()));
        rewardRepository.save(reward);
    }
}
