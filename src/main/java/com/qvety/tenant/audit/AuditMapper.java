package com.qvety.tenant.audit;

import org.mapstruct.Mapper;

@Mapper
public interface AuditMapper {
    AuditEntryDto toDto(AuditEntry entry);
}
