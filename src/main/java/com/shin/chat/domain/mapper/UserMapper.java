package com.shin.chat.domain.mapper;

import com.shin.chat.domain.dto.UserDto;
import com.shin.chat.domain.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

// 매퍼임을 알리는 어노테이션.
// componentModel = "spring"으로 spring container에 bean으로 등록해준다.
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper userMapperInstance = Mappers.getMapper(UserMapper.class);

    // @Mapping(target = "name", source = "name") 필드명이 다를 경우 mapping을 해줘야 한다. (예시)
    public UserDto toDto(UserEntity userEntity);

    //@Mapping(target="no", ignore = true) 특성 변수 값을 제외하고 매핑 할 경우 ignore 사용 가능.
    public UserEntity toEntity(UserDto userDto);
}
