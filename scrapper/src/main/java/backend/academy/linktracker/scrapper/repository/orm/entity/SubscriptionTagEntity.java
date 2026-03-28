package backend.academy.linktracker.scrapper.repository.orm.entity;

import backend.academy.linktracker.scrapper.repository.orm.id.SubscriptionTagId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_tags")
@IdClass(SubscriptionTagId.class)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class SubscriptionTagEntity {
    @Id
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Id
    @Column(name = "tag")
    private String tag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", insertable = false, updatable = false)
    private SubscriptionEntity subscription;

    public SubscriptionTagEntity(SubscriptionEntity subscription, String tag) {
        this.subscription = subscription;
        this.subscriptionId = subscription.getId();
        this.tag = tag;
    }
}
