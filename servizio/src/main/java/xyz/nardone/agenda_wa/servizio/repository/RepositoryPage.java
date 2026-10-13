package xyz.nardone.agenda_wa.servizio.repository;

import java.util.List;

public record RepositoryPage<T>(List<T> items, long total) {}
