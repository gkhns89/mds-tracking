package com.gcodes.aacctracker.repository;

import com.gcodes.aacctracker.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    List<SubscriptionPlan> findByIsActiveTrue();

    Optional<SubscriptionPlan> findByName(String name);

    Optional<SubscriptionPlan> findByIdAndIsActiveTrue(Long id);
}