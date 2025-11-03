package de.htwg.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
public class Accommodation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private String name;

    private Double pricePerNight;

    private Float rating;

    private String notes;

    private String accommodationImageUrl;

    @OneToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;
}
