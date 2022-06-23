package tl.develoveper.lambda.shipping.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import tl.develoveper.lambda.shipping.dao.ShippingDao;
import tl.develoveper.lambda.shipping.entity.Consignment;
import tl.develoveper.lambda.shipping.entity.ConsignmentCheckIn;
import tl.develoveper.lambda.shipping.entity.ConsignmentItem;

import java.util.Comparator;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShippingService {
    private SessionFactory sessionFactory;
    private ShippingDao shippingDao;

    public String createConsignment(Consignment consignment) {
        try (Session session = sessionFactory.openSession()) {
            consignment.setDelivered(false);
            consignment.setId(UUID.randomUUID().toString());
            shippingDao.save(session, consignment);
            return consignment.getId();
        }
    }

    public Consignment view(String consignmentId) {
        try (Session session = sessionFactory.openSession()) {
            return shippingDao.find(session, consignmentId)
                    .orElseGet(Consignment::new);
        }
    }

    public void addItem(String consignmentId, ConsignmentItem item) {
        try (Session session = sessionFactory.openSession()) {
            shippingDao.find(session, consignmentId)
                    .ifPresent(consignment -> addItem(session, consignment, item));
        }
    }

    private void addItem(Session session, Consignment consignment, ConsignmentItem item) {
        consignment.getItems()
                .add(item);
        shippingDao.save(session, consignment);
    }

    public void checkIn(String consignmentId, ConsignmentCheckIn checkin) {
        try (Session session = sessionFactory.openSession()) {
            shippingDao.find(session, consignmentId)
                    .ifPresent(consignment -> checkIn(session, consignment, checkin));
        }
    }

    private void checkIn(Session session, Consignment consignment, ConsignmentCheckIn checkin) {
        consignment.getCheckins().add(checkin);
        consignment.getCheckins().sort(Comparator.comparing(ConsignmentCheckIn::getTimeStamp));
        if (checkin.getLocation().equals(consignment.getDestination())) {
            consignment.setDelivered(true);
        }
        shippingDao.save(session, consignment);
    }
}
