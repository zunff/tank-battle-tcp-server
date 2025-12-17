package com.zunf.tankbattletcpserver.handler.mapper;

import com.google.protobuf.Descriptors;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.Condition;

/**
 * Proto 转换公共配置：仅保留规则配置，删除无效的泛型映射方法
 */
@MapperConfig(
        componentModel = "spring", // 生成 Spring Bean
        unmappedTargetPolicy = ReportingPolicy.WARN, // 未映射字段仅警告
        unmappedSourcePolicy = ReportingPolicy.WARN // 源字段未映射仅警告
)
public interface CommonProtoMapperConfig {

}