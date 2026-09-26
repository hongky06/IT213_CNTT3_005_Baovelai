package org.example.hackathon_de05.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.hackathon_de05.model.constant.MembershipGroup;

@Entity
@Table(name = "reservation_users")
@Getter
@Setter
@NoArgsConstructor
public class ReservationUser {
    @Id
    @Column(name = "user_id", length = 80)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_group", nullable = false, length = 20)
    private MembershipGroup membershipGroup;

    public ReservationUser(String userId, MembershipGroup membershipGroup) {
        this.userId = userId;
        this.membershipGroup = membershipGroup;
    }
}
