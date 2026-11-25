package com.corp.DeskHelper.chamado;

import org.springframework.data.mongodb.repository.MongoRepository;

interface ChamadoRepository extends MongoRepository<Chamado, String> {}
