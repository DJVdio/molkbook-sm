package com.molkbook.repository;

import com.molkbook.entity.UserShade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserShadeRepository extends JpaRepository<UserShade, Long> {

    List<UserShade> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
