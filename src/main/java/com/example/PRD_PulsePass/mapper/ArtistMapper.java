package com.example.PRD_PulsePass.mapper;

import com.example.PRD_PulsePass.domain.Artist;
import com.example.PRD_PulsePass.dto.response.ArtistResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArtistMapper {
    ArtistResponse toResponse(Artist artist);
}
