package com.company.coffee.view.order;

import com.company.coffee.entity.Discount;
import com.company.coffee.entity.Order;
import com.company.coffee.view.main.MainView;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Route(value = "orders/:id", layout = MainView.class)
@ViewController(id = "Order_.detail")
@ViewDescriptor(path = "order-detail-view.xml")
@EditedEntityContainer("orderDc")
public class OrderDetailView extends StandardDetailView<Order> {
    @ViewComponent
    private InstanceContainer<Order> orderDc;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private Notifications notifications;
    @ViewComponent
    private EntityComboBox<Discount> discountField;
    @ViewComponent
    private JmixButton closeButton;
    @ViewComponent
    private JmixButton confirmOrder;

    @Subscribe
    public void onInit(final InitEvent event) {
        discountField.setItemLabelGenerator(discount ->
                discount.getName() + " - " + discount.getPercentage() + "%"
        );
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        Order order = orderDc.getItem();

        if ("Confirmed".equals(order.getStatus())) {
            discountField.setVisible(false);
            confirmOrder.setVisible(false);
            closeButton.setVisible(false);
        }
    }

    @Subscribe("discountField")
    public void onDiscountFieldComponentValueChange(final AbstractField.ComponentValueChangeEvent<EntityComboBox<Discount>, Discount> event) {
        calculateFinalTotal();
    }

    private void calculateFinalTotal() {
        Order order = orderDc.getItem();
        if (order.getLinks() == null) return;

        BigDecimal subTotal = order.getLinks().stream()
                .map(link -> {
                    BigDecimal price = link.getProduct().getPrice() != null ? link.getProduct().getPrice() : BigDecimal.ZERO;

                    BigDecimal pDisc = (link.getProduct().getDiscount() != null) ? link.getProduct().getDiscount().getPercentage() : BigDecimal.ZERO;
                    BigDecimal priceAfterPDisc = price.multiply(BigDecimal.valueOf(100).subtract(pDisc))
                            .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

                    return priceAfterPDisc.multiply(BigDecimal.valueOf(link.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalTotal = subTotal;
        if (order.getDiscount() != null && order.getDiscount().getPercentage() != null) {
            BigDecimal billPercent = order.getDiscount().getPercentage();

            BigDecimal billDiscountFactor = BigDecimal.valueOf(100).subtract(billPercent);
            finalTotal = (subTotal.multiply(billDiscountFactor)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        }
        order.setTotal(finalTotal);
    }

    @Subscribe("confirmOrder")
    public void onConfirmOrderClick(final ClickEvent<JmixButton> event) {
        Order order = orderDc.getItem();

        order.setStatus("Confirmed");
        dataManager.save(order);
        notifications.show("Order has been confirmed with price: " + order.getTotal());

        closeWithSave();
    }
}