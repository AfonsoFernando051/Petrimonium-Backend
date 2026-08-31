package com.jf.PetApp.infrastructure.repository.gamification;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.ActivityLogJpaEntity;

@Repository
public interface ActivityLogJpaRepository extends JpaRepository<ActivityLogJpaEntity, Long> {

    boolean existsByUserIdAndActivityDate(Long userId, LocalDate activityDate);

    @Query("select a.activityDate from ActivityLogJpaEntity a where a.userId = :userId order by a.activityDate desc")
    List<LocalDate> findActivityDatesByUserIdOrderByActivityDateDesc(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
