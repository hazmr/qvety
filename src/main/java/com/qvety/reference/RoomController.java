package com.qvety.reference;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/reference/rooms", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "reference")
public class RoomController extends ReferenceController<RoomRequest, RoomDto> {

    public RoomController(RoomService service) {
        super(service);
    }
}
