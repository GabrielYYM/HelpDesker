package com.corp.DeskHelper.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import com.corp.DeskHelper.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByCode(String code);

    boolean existsByCode(String code);

    @Query("{ '_id': ?0, 'active': true, 'firstAccess': true, 'password': ?1 }")
    @Update("{ '$set': { 'password': ?2, 'firstAccess': false } }")
    long completeFirstAccess(String id, String temporaryHash, String newHash);

    @Query("{ '_id': ?0, 'active': true, 'firstAccess': true, 'password': ?1 }")
    @Update("{ '$set': { 'password': ?2 } }")
    long replaceTemporaryPassword(String id, String previousHash, String temporaryHash);
}
