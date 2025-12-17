package com.zunf.tankbattletcpserver.annotation;

import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解：批量忽略Proto通用字段（写一次，所有方法复用）
 */
@Target({ElementType.METHOD}) // 仅作用于方法
@Retention(RetentionPolicy.CLASS) // 编译期生效
@Mappings({
    @Mapping(target = "nameBytes", ignore = true),
    @Mapping(target = "unknownFields", ignore = true),
    @Mapping(target = "allFields", ignore = true),
    @Mapping(target = "clearField", ignore = true),
    @Mapping(target = "mergeFrom", ignore = true),
    @Mapping(target = "clearOneof", ignore = true),
    @Mapping(target = "mergeUnknownFields", ignore = true)
})
public @interface IgnoreProtoCommonFields {
}