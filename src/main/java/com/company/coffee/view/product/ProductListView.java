package com.company.coffee.view.product;

import com.company.coffee.entity.Order;
import com.company.coffee.entity.OrderProductLink;
import com.company.coffee.entity.Product;
import com.company.coffee.entity.User;
import com.company.coffee.view.main.MainView;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.app.inputdialog.DialogActions;
import io.jmix.flowui.app.inputdialog.DialogOutcome;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;


@Route(value = "products", layout = MainView.class)
@ViewController(id = "Product.list")
@ViewDescriptor(path = "product-list-view.xml")
@LookupComponent("productsDataGrid")
@DialogMode(width = "64em")
public class ProductListView extends StandardListView<Product> {
    @Autowired
    protected UiComponents uiComponents;
    @Autowired
    protected DataManager dataManager;

    @ViewComponent
    private DataGrid<Product> productsDataGrid;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private Dialogs dialogs;

    @Autowired
    private Notifications notifications;

    @Subscribe
    public void onInit(final InitEvent event) {
        setupAddToCartColumn();
    }

    private void setupAddToCartColumn() {
        boolean isAdmin = currentAuthentication.getUser().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ROLE_system-full-access"));

        DataGrid.Column<Product> column = productsDataGrid.getColumnByKey("addToCartColumn");

        if (column != null) {
            if (isAdmin) {
                column.setVisible(false);
            } else {
                column.setRenderer(new ComponentRenderer<>(product -> {
                    JmixButton plusButton = uiComponents.create(JmixButton.class);
                    plusButton.setIcon(VaadinIcon.PLUS.create());
                    plusButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

                    plusButton.addClickListener(e -> {
                        dialogs.createInputDialog(this)
                                .withHeader("How much " + product.getName() + " do you want to add?")
                                .withParameters(
                                        InputParameter.intParameter("quantity")
                                                .withLabel("Quantity")
                                                .withDefaultValue(1)
                                )
                                .withActions(DialogActions.OK_CANCEL)
                                .withCloseListener(closeEvent -> {
                                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                                        Integer qty = closeEvent.getValue("quantity");
                                        if (qty != null && qty > 0) {
                                            processAddToCart(product, qty);
                                        }
                                    }
                                })
                                .open();
                    });

                    return plusButton;
                }));
            }
        }
    }

    private void processAddToCart(Product product, Integer quantity) {
        User user = (User) currentAuthentication.getUser();
        String userPhone = user.getPhone();

        Order draftOrder = dataManager.load(Order.class)
                .query("select e from Order_ e where e.customer_phone = :phone and e.status = :status")
                .parameter("phone", userPhone)
                .parameter("status", "Draft")
                .fetchPlan(fp -> {
                    fp.addFetchPlan(FetchPlan.BASE);
                    fp.add("links", linkFp -> {
                        linkFp.addFetchPlan(FetchPlan.BASE);
                        linkFp.add("product", FetchPlan.BASE);
                    });
                })
                .optional()
                .orElse(null);

        if (draftOrder == null) {
            draftOrder = dataManager.create(Order.class);
            draftOrder.setLinks(new ArrayList<>());
            draftOrder.setCustomer_phone(userPhone);
            draftOrder.setStatus("Draft");
        }

        OrderProductLink link = draftOrder.getLinks().stream()
                .filter(l -> l.getProduct().equals(product))
                .findFirst()
                .orElse(null);

        if (link == null) {
            link = dataManager.create(OrderProductLink.class);
            link.setProduct(product);
            link.setOrder(draftOrder);
            link.setQuantity(quantity);
            draftOrder.getLinks().add(link);
        } else {
            link.setQuantity(link.getQuantity() + quantity);
        }

        BigDecimal total = calculateTotal(draftOrder.getLinks());
        draftOrder.setTotal(total);

        dataManager.save(new SaveContext().saving(draftOrder));

        notifications.show("Cart updated: " + product.getName() + " x" + quantity + ". Total: " + total.toPlainString());
    }

    private BigDecimal calculateTotal(List<OrderProductLink> links) {
        if (links == null) return BigDecimal.ZERO;
        return links.stream()
                .map(l -> {
                    Product p = l.getProduct();
                    BigDecimal price = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
                    BigDecimal qty = BigDecimal.valueOf(l.getQuantity() != null ? l.getQuantity() : 1);

                    BigDecimal percent = (p.getDiscount() != null && p.getDiscount().getPercentage() != null)
                            ? p.getDiscount().getPercentage()
                            : BigDecimal.ZERO;

                    BigDecimal discountFactor = BigDecimal.valueOf(100).subtract(percent);

                    BigDecimal amountBeforeDivision = price.multiply(qty).multiply(discountFactor);

                    return amountBeforeDivision.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }
}