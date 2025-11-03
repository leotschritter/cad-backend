package de.htwg.persistence.repository;

import de.htwg.persistence.entity.Accommodation;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AccommodationRepository implements PanacheRepository<Accommodation> {
}
