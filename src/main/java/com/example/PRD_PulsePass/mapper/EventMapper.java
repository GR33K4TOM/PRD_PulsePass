package com.example.PRD_PulsePass.mapper;

import com.example.PRD_PulsePass.domain.Event;
import com.example.PRD_PulsePass.dto.response.EventResponse;
import com.example.PRD_PulsePass.dto.response.EventSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ArtistMapper.class})
public interface EventMapper {
    
    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    EventResponse toResponse(Event event);

    @Mapping(target = "venueCode", source = "venue.code")
    EventSummaryResponse toSummary(Event event);
}
