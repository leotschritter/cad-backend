package de.htwg.persistence.repository;

import de.htwg.persistence.entity.Itinerary;
import de.htwg.persistence.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ItineraryRepository implements PanacheRepository<Itinerary> {

    public List<Itinerary> findByUser(User user) {
        return find("user", user).list();
    }

    public List<Itinerary> findByUserId(Long userId) {
        return find("user.id", userId).list();
    }

    public List<Itinerary> findByUserEmail(String email) {
        return find("user.email", email).list();
    }
}
