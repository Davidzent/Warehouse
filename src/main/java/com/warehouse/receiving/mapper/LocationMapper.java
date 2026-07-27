package com.warehouse.receiving.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.warehouse.receiving.domain.Location;

@Mapper
public interface LocationMapper {

    List<Location> findAll();
}
