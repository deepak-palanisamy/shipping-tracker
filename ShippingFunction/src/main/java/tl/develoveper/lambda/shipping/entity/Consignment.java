package tl.develoveper.lambda.shipping.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.FetchType.EAGER;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "consignment")
@Table(name = "consignment")
public class Consignment {

    @Id
    @Column(name = "consignment_id")
    private String id;

    @Column(name = "source")
    private String source;

    @Column(name = "destination")
    private String destination;

    @Column(name = "delivered", columnDefinition = "boolean")
    private boolean isDelivered;

    @ElementCollection(fetch = EAGER)
    @CollectionTable(name = "consignment_item", joinColumns = @JoinColumn(name = "consignment_id"))
    @OrderColumn(name = "item_index")
    private List<ConsignmentItem> items = new ArrayList<>();

    @ElementCollection(fetch = EAGER)
    @CollectionTable(name = "consignment_checkin", joinColumns = @JoinColumn(name = "consignment_id"))
    @OrderColumn(name = "checkin_index")
    private List<ConsignmentCheckIn> checkins = new ArrayList<>();
}
