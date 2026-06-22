package com.company.coffee.view.main;

import com.company.coffee.entity.Discount;
import com.company.coffee.entity.DiscountType;
import com.company.coffee.view.product.ProductListView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.app.main.StandardMainView;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import io.jmix.flowui.ViewNavigators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

@Route("")
@ViewController(id = "MainView")
@ViewDescriptor(path = "main-view.xml")
public class MainView extends StandardMainView {
    @ViewComponent
    private DataGrid<Discount> activeDiscountsGrid;

    @Autowired
    private ViewNavigators navigators;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @ViewComponent
    private VerticalLayout adminDashboardHeader;

    @ViewComponent
    private VerticalLayout customerPromotionArea;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        checkUserRoleAndSetupUI();
    }

    private void checkUserRoleAndSetupUI() {
        Collection<? extends GrantedAuthority> authorities = currentAuthentication.getUser().getAuthorities();

        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().contains("ROLE_system-full-access"));
        if (isAdmin) {
            adminDashboardHeader.setVisible(true);
            customerPromotionArea.setVisible(false);
        } else {
            customerPromotionArea.setVisible(true);
            adminDashboardHeader.setVisible(false);
        }
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        activeDiscountsGrid.addColumn(discount -> {
            DiscountType type = discount.getType();
            if (type == null) return "";
            return type == DiscountType.TOTAL ? "On total bill" : "On specific products";
        }).setHeader("Discount Type").setFlexGrow(1);

        activeDiscountsGrid.removeColumn(activeDiscountsGrid.getColumnByKey("type"));
    }

    @Subscribe("orderRedirect")
    public void onOrderRedirect(ClickEvent<JmixButton> event) {
        navigators.view(this, ProductListView.class).navigate();
    }
}
