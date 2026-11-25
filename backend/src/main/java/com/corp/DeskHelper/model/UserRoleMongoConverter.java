package com.corp.DeskHelper.model;

import org.springframework.data.convert.PropertyValueConverter;
import org.springframework.data.mongodb.core.convert.MongoConversionContext;

import com.corp.DeskHelper.model.enums.UserRole;

public class UserRoleMongoConverter implements PropertyValueConverter<UserRole, String, MongoConversionContext> {

    @Override
    public UserRole read(String value, MongoConversionContext context) {
        return switch (value) {
            case "ADMINISTRADOR" -> UserRole.ADM;
            case "USUARIO" -> UserRole.USER;
            case "GERENTE" -> UserRole.IT;
            default -> UserRole.valueOf(value);
        };
    }

    @Override
    public String write(UserRole role, MongoConversionContext context) {
        return role.name();
    }
}
