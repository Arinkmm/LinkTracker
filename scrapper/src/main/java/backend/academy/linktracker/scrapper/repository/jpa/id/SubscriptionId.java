package backend.academy.linktracker.scrapper.repository.jpa.id;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionId implements Serializable {
    private Long userId;
    private Long linkId;
}
