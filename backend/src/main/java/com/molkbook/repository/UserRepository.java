package com.molkbook.repository;

import com.molkbook.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findBySecondmeToken(String secondmeToken);

    @Query(value = "SELECT * FROM users WHERE id != :excludeUserId ORDER BY RAND()", nativeQuery = true)
    List<User> findRandomUsersExcluding(Long excludeUserId);

    @Query(value = "SELECT * FROM users WHERE id NOT IN (:excludeUserIds) ORDER BY RAND()", nativeQuery = true)
    List<User> findRandomUsersExcludingMultiple(List<Long> excludeUserIds);

    @Query(value = "SELECT * FROM users ORDER BY RAND()", nativeQuery = true)
    List<User> findAllRandomUsers();

    @Query("SELECT u FROM User u ORDER BY u.updatedAt DESC")
    List<User> findActiveUsers();
}
