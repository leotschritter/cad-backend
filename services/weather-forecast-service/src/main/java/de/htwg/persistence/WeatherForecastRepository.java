package de.htwg.persistence;

import de.htwg.entity.WeatherForecast;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WeatherForecastRepository implements PanacheRepository<WeatherForecast> {

    public List<WeatherForecast> findByLocation(String location) {
        return find("LOWER(location) = LOWER(?1)", location).list();
    }

    public List<WeatherForecast> findByCoordinates(Double latitude, Double longitude) {
        return find("latitude = ?1 and longitude = ?2", latitude, longitude)
                .list();
    }
}
