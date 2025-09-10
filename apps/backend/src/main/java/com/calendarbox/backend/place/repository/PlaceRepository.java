package com.calendarbox.backend.place.repository;

import com.calendarbox.backend.place.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findByProviderAndProviderPlaceKey(String provider, String providerPlaceKey);
}
