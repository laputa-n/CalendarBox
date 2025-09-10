package com.calendarbox.backend.place.domain;

import com.calendarbox.backend.place.dto.response.PlacePreview;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Place {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long id;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_place_key")
    private String providerPlaceKey;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "link")
    private String link;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "address")
    private String address;

    @Column(name = "road_address")
    private String roadAddress;

    @Column(name = "lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    private Place(String provider, String providerPlaceKey, String title, String link,
                  String category, String description, String address, String roadAddress,
                  BigDecimal lat, BigDecimal lng) {
        this.provider = Objects.requireNonNull(provider);
        this.providerPlaceKey = providerPlaceKey;
        this.title = Objects.requireNonNull(title);
        this.link = link;
        this.category = category;
        this.description = description;
        this.address = address;
        this.roadAddress = roadAddress;
        this.lat = Objects.requireNonNull(lat);
        this.lng = Objects.requireNonNull(lng);
    }

    public static Place fromPreview(PlacePreview p) {
        return Place.builder()
                .provider(p.provider())
                .providerPlaceKey(p.providerPlaceKey())
                .title(p.title())
                .link(p.link())
                .category(p.category())
                .description(p.description())
                .address(p.address())
                .roadAddress(p.roadAddress())
                .lat(BigDecimal.valueOf(p.lat()))
                .lng(BigDecimal.valueOf(p.lng()))
                .build();
    }


    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
