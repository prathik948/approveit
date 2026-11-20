package com.approveit.approveit.repository;

import com.approveit.approveit.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
