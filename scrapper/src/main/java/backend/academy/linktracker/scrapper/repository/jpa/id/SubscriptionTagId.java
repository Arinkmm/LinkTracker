package backend.academy.linktracker.scrapper.repository.jpa.id;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SubscriptionTagId implements Serializable {
    private Long userId;
    private Long linkId;
    private String tag;
}
