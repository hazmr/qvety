package com.qvety.reference;

import org.springframework.stereotype.Service;

/** Rooms. Every rule lives in {@link ReferenceService}; this only says what a room is. */
@Service
public class RoomService extends ReferenceService<Room, RoomRequest, RoomDto> {

    private final RoomMapper mapper;

    public RoomService(RoomRepository repository, RoomMapper mapper) {
        super(repository);
        this.mapper = mapper;
    }

    @Override
    protected Room newEntity() {
        return new Room();
    }

    @Override
    protected void apply(RoomRequest request, Room room) {
        mapper.apply(request, room);
    }

    @Override
    protected RoomDto toDto(Room room) {
        return mapper.toDto(room);
    }

    @Override
    protected String name(RoomRequest request) {
        return request.name();
    }
}
