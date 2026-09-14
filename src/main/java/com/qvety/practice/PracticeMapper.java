package com.qvety.practice;

import org.mapstruct.Mapper;

@Mapper
public interface PracticeMapper {
    PracticeDto toDto(Practice practice);
}
