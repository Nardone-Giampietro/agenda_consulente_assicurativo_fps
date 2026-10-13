package xyz.nardone.agenda_wa.servizio.DTO.common;

import java.util.List;

public record PageDTO<T>(List<T> items, long total, int page, int size) {}
