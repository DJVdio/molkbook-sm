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

    @Query("SELECT u FROM User u WHERE u.id != :excludeUserId ORDER BY FUNCTION('RAND')")
    List<User> findRandomUsersExcluding(Long excludeUserId);

    @Query("SELECT u FROM User u WHERE u.id NOT IN :excludeUserIds ORDER BY FUNCTION('RAND')")
    List<User> findRandomUsersExcludingMultiple(List<Long> excludeUserIds);

    @Query("SELECT u FROM User u ORDER BY u.updatedAt DESC")
    List<User> findActiveUsers();
}
