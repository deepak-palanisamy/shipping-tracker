package tl.develoveper.lambda.shipping.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ConsignmentItem {

    @Column(name = "location")
    private String location;

    @Column(name = "description")
    private String description;

    @Column(name = "timestamp")
    private String timeStamp;

}
