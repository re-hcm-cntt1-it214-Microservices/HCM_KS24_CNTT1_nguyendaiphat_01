package org.example.orderservice.models.services.impl;

import org.example.orderservice.clients.ProductClient;
import org.example.orderservice.models.constants.OrderStatus;
import org.example.orderservice.models.dto.requests.CreateOrderDetailRequest;
import org.example.orderservice.models.dto.requests.CreateOrderRequest;
import org.example.orderservice.models.dto.responses.OrderDetailResponse;
import org.example.orderservice.models.dto.responses.OrderResponse;
import org.example.orderservice.models.dto.responses.ProductResponse;
import org.example.orderservice.models.entities.Order;
import org.example.orderservice.models.entities.OrderDetail;
import org.example.orderservice.models.repositories.OrderDetailRepository;
import org.example.orderservice.models.repositories.OrderRepository;
import org.example.orderservice.models.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductClient productClient;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetail> details = new ArrayList<>();

        order = orderRepository.save(order);

        for (CreateOrderDetailRequest detailReq : request.getDetails()) {
            ProductResponse product = productClient.getProductById(detailReq.getProductId());

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProductId(product.getId());
            detail.setQuantity(detailReq.getQuantity());
            detail.setUnitPrice(product.getPrice());

            BigDecimal detailTotal = product.getPrice().multiply(BigDecimal.valueOf(detailReq.getQuantity()));
            totalAmount = totalAmount.add(detailTotal);

            details.add(orderDetailRepository.save(detail));
        }

        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        OrderResponse response = mapToResponse(order, details);

        if (kafkaTemplate != null) {
            kafkaTemplate.send("order-created-topic", response);
        }

        return response;
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        List<OrderDetail> details = orderDetailRepository.findByOrderId(id);
        return mapToResponse(order, details);
    }

    private OrderResponse mapToResponse(Order order, List<OrderDetail> details) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setOrderDate(order.getOrderDate());

        List<OrderDetailResponse> detailResponses = details.stream().map(d -> {
            OrderDetailResponse dr = new OrderDetailResponse();
            dr.setId(d.getId());
            dr.setProductId(d.getProductId());
            dr.setQuantity(d.getQuantity());
            dr.setUnitPrice(d.getUnitPrice());
            return dr;
        }).collect(Collectors.toList());

        response.setDetails(detailResponses);
        return response;
    }
}
