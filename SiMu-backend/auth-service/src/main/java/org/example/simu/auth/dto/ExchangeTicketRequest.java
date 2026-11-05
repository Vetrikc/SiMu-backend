package org.example.simu.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeTicketRequest(@NotBlank(message = "Ticket must not be blank") String ticket) {
}
