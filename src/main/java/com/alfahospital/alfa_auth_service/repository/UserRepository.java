package com.alfahospital.alfa_auth_service.repository;

import com.alfahospital.alfa_auth_service.domain.Role;
import com.alfahospital.alfa_auth_service.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdNumber(String idNumber);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " u.idNumber         LIKE CONCAT('%', :q, '%'))")
    List<User> searchPacientes(@Param("role") Role role, @Param("q") String q);
}