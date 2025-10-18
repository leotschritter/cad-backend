package de.htwg.persistence.repository;

import de.htwg.persistence.entity.Itinerary;
import de.htwg.persistence.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.time.LocalDate;
import java.util.ArrayList;
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

    public List<Itinerary> searchItineraries(
            final String userName,
            final String userEmail,
            final String title,
            final String destination,
            final String description,
            final LocalDate startDateFrom,
            final LocalDate startDateTo) {

        final EntityManager em = getEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Itinerary> cq = cb.createQuery(Itinerary.class);
        final Root<Itinerary> root = cq.from(Itinerary.class);

        final List<Predicate> predicates = new ArrayList<>();

        if (userName != null && !userName.isBlank()) {
            predicates.add(likeIgnoreCase(cb, root.get("user").get("name"), userName));
        }

        if (userEmail != null && !userEmail.isBlank()) {
            predicates.add(likeIgnoreCase(cb, root.get("user").get("email"), userEmail));
        }

        if (title != null && !title.isBlank()) {
            predicates.add(likeIgnoreCase(cb, root.get("title"), title));
        }

        if (destination != null && !destination.isBlank()) {
            predicates.add(likeIgnoreCase(cb, root.get("destination"), destination));
        }

        if (description != null && !description.isBlank()) {
            predicates.add(cb.or(
                    likeIgnoreCase(cb, root.get("shortDescription"), description),
                    likeIgnoreCase(cb, root.get("detailedDescription"), description)
            ));
        }

        if (startDateFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDateFrom));
        }

        if (startDateTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), startDateTo));
        }

        cq.select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.desc(root.get("startDate")));

        final TypedQuery<Itinerary> query = em.createQuery(cq);
        query.setMaxResults(100);

        return query.getResultList();
    }

    private Predicate likeIgnoreCase(
            CriteriaBuilder cb,
            Expression<String> path,
            String value
    ) {
        final String like = "%" + value.toLowerCase().trim() + "%";
        return cb.like(cb.lower(path), like);
    }
}
