package org.example.hackathon_de05.repository;

import org.example.hackathon_de05.model.entity.ReservationUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationUserRepository extends JpaRepository<ReservationUser, String> {
}
