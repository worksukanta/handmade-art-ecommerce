package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.entity.Shipment;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.OrderItemRepository;
import com.handmadeart.ecommerce.repository.PaymentRepository;
import com.handmadeart.ecommerce.repository.ShipmentRepository;
import com.handmadeart.ecommerce.service.AdminOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderDiscoveryServiceTest {
    @Mock CustomerOrderRepository orderRepository;
    @Mock OrderItemRepository itemRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ShipmentRepository shipmentRepository;

    private AdminOrderService service() {
        return new AdminOrderService(orderRepository, itemRepository, paymentRepository, shipmentRepository);
    }

    @Test
    void existingOrder_returnsOnlyItsPayments() {
        when(orderRepository.findById(7L)).thenReturn(Optional.of(mock(CustomerOrder.class)));
        when(paymentRepository.findByOrderId(7L)).thenReturn(List.of(mock(Payment.class), mock(Payment.class)));
        assertEquals(2, service().getOrderPayments(7L).size());
        verify(paymentRepository).findByOrderId(7L);
    }

    @Test
    void existingOrderWithoutPayments_returnsEmptyList() {
        when(orderRepository.findById(7L)).thenReturn(Optional.of(mock(CustomerOrder.class)));
        when(paymentRepository.findByOrderId(7L)).thenReturn(List.of());
        assertEquals(List.of(), service().getOrderPayments(7L));
    }

    @Test
    void unknownOrderPayments_returns404BeforeChildQuery() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service().getOrderPayments(99L));
        verify(paymentRepository, never()).findByOrderId(99L);
    }

    @Test
    void existingOrderShipment_returnsAssociatedShipment() {
        when(orderRepository.findById(7L)).thenReturn(Optional.of(mock(CustomerOrder.class)));
        when(shipmentRepository.findByOrderId(7L)).thenReturn(Optional.of(mock(Shipment.class)));
        service().getOrderShipment(7L);
        verify(shipmentRepository).findByOrderId(7L);
    }

    @Test
    void existingOrderWithoutShipment_returns404() {
        when(orderRepository.findById(7L)).thenReturn(Optional.of(mock(CustomerOrder.class)));
        when(shipmentRepository.findByOrderId(7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service().getOrderShipment(7L));
    }

    @Test
    void unknownOrderShipment_returns404BeforeChildQuery() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service().getOrderShipment(99L));
        verify(shipmentRepository, never()).findByOrderId(99L);
    }
}
