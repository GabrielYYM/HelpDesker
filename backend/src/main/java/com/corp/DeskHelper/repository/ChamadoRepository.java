package com.corp.DeskHelper.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.corp.DeskHelper.model.Chamado;

@Repository
public interface ChamadoRepository extends MongoRepository<Chamado, String> {
}
