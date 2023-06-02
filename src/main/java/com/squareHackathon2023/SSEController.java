package com.squareHackathon2023;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SSEController {

    private final Map<Integer, SseEmitter> emitters = new HashMap<>();

    @GetMapping("/seat-availability/{seatNumber}")
    public SseEmitter seatAvailability(@PathVariable int seatNumber) {
        SseEmitter emitter = new SseEmitter();
        emitters.put(seatNumber, emitter);
        emitter.onCompletion(() -> emitters.remove(seatNumber, emitter));
        return emitter;
    }

    public void notifySeatAvailabilityChange(Seat seat) {
        SseEmitter emitter = emitters.get(seat.getNum());
        if (emitter != null) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .data(seat)
                        .id(String.valueOf(seat.getNum()))
                        .name("seat-update");
                emitter.send(event);
            } catch (IOException e) {
                // Handle exception
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
