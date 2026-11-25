package com.corp.DeskHelper.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.corp.DeskHelper.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByCode(String code);

    boolean existsByCode(String code);

    Optional<User> findFirstByOrderByCodeDesc();
}
