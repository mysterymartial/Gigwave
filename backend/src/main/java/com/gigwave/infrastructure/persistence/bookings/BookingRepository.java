package com.gigwave.infrastructure.persistence.bookings;

import com.gigwave.domain.bookings.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends MongoRepository<Booking, UUID> {
    List<Booking> findByMusicianId(UUID musicianId);
    List<Booking> findByGigId(UUID gigId);
    
    @Query("{ 'gigId': { $in: ?0 } }")
    List<Booking> findByGigIds(List<UUID> gigIds);
    
    @Query("{ $or: [ { 'musicianId': ?0 }, { 'gigId': { $in: ?1 } } ] }")
    List<Booking> findByUserIdOrGigIds(UUID userId, List<UUID> gigIds);
}
