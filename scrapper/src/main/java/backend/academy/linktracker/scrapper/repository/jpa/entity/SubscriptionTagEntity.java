package backend.academy.linktracker.scrapper.repository.jpa.entity;

import backend.academy.linktracker.scrapper.repository.jpa.id.SubscriptionTagId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_tags")
@IdClass(SubscriptionTagId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionTagEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "link_id")
    private Long linkId;

    @Id
    @Column(name = "tag")
    private String tag;
}
