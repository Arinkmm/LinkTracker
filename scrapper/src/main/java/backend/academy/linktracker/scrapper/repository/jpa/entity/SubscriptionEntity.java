package backend.academy.linktracker.scrapper.repository.jpa.entity;

import backend.academy.linktracker.scrapper.repository.jpa.id.SubscriptionId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscriptions")
@IdClass(SubscriptionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "link_id")
    private Long linkId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumns({
        @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
        @JoinColumn(name = "link_id", referencedColumnName = "link_id")
    })
    private List<SubscriptionTagEntity> tags = new ArrayList<>();
}
