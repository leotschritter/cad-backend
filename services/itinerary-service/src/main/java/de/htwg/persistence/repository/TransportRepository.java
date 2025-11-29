package de.htwg.persistence.repository;

import de.htwg.persistence.entity.Transport;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransportRepository implements PanacheRepository<Transport> {
}
