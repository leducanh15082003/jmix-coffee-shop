package com.company.coffee.view.order;

import com.company.coffee.entity.Order;
import com.company.coffee.entity.User;
import com.company.coffee.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.kit.action.BaseAction;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import io.jmix.reportsflowui.runner.UiReportRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;


@Route(value = "orders", layout = MainView.class)
@ViewController(id = "Order_.list")
@ViewDescriptor(path = "order-list-view.xml")
@LookupComponent("ordersDataGrid")
@DialogMode(width = "64em")
public class OrderListView extends StandardListView<Order> {
    @Autowired
    private DataManager dataManager;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private Notifications notifications;

    @Autowired
    private Dialogs dialogs;

    @Autowired
    private UiReportRunner uiReportRunner;

    @ViewComponent
    private JmixButton createButton;

    @ViewComponent
    private JmixButton editButton;

    @ViewComponent
    private JmixButton removeButton;

    @ViewComponent
    private JmixButton printAllBtn;

    @ViewComponent
    private JmixButton confirmOrder;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        boolean isAdmin = currentAuthentication.getUser().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ROLE_system-full-access"));

        if (!isAdmin) {
            createButton.setVisible(false);
            editButton.setVisible(false);
            removeButton.setVisible(false);
            printAllBtn.setVisible(false);
        } else {
            confirmOrder.setVisible(false);
        }
    }

    @Subscribe("confirmOrder")
    public void onConfirmOrderClick(final ClickEvent<JmixButton> event) {
        User user = (User) currentAuthentication.getUser();
        String userPhone = user.getPhone();

        Order draftOrder = dataManager.load(Order.class)
                .query("select e from Order_ e where e.customer_phone = :phone and e.status = :status")
                .parameter("phone", userPhone)
                .parameter("status", "Draft")
                .optional()
                .orElse(null);

        if (draftOrder != null) {
            draftOrder.setStatus("Confirmed");
            dataManager.save(draftOrder);
            notifications.show("Your order has been confirmed!");
        }
    }

    @Subscribe("ordersDataGrid.printAll")
    public void onPrintAll(final ActionPerformedEvent event) {
        dialogs.createOptionDialog()
                .withHeader("Pick time period")
                .withText("How long this order will be printed in")
                .withActions(
                        new BaseAction("day")
                                .withText("Day")
                                .withHandler(e -> generateReport(1, ChronoUnit.DAYS)),
                        new BaseAction("week")
                                .withText("Week")
                                .withHandler(e -> generateReport(7, ChronoUnit.DAYS)),
                        new BaseAction("month")
                                .withText("Month")
                                .withHandler(e -> generateReport(1, ChronoUnit.MONTHS)),
                        new BaseAction("quarter")
                                .withText("Quarter")
                                .withHandler(e -> generateReport(3, ChronoUnit.MONTHS)),
                        new BaseAction("cancel")
                                .withText("Cancel")
                                .withVariant(ActionVariant.DANGER)
                )
                .open();
    }

    private void generateReport(int amount, ChronoUnit unit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startResult ;
        LocalDateTime endResult = now;

        if (unit == ChronoUnit.MONTHS && amount == 1) {
            if (now.getDayOfMonth() == 1) {
                startResult = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                endResult = now.withHour(0).withMinute(0).withSecond(0).minusMinutes(1);
            } else {
                startResult = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            }
        } else if (unit == ChronoUnit.DAYS && amount == 7) {
            if (now.getDayOfWeek() == DayOfWeek.MONDAY) {
                startResult = now.minusWeeks(1).with(DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0);
                endResult = now.minusDays(1).withHour(23).withMinute(59).withSecond(59);
            } else {
                startResult = now.with(DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0);
            }
        } else if (unit == ChronoUnit.MONTHS && amount == 3) {
            if (now.getMonthValue() <= 3) {
                startResult = now.withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            } else if (now.getMonthValue() <= 6) {
                startResult = now.withMonth(4).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            } else if (now.getMonthValue() <= 9) {
                startResult = now.withMonth(7).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            } else {
                startResult = now.withMonth(10).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            }
        } else {
            startResult = now.with(DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }

        Date startTime = Date.from(startResult.atZone(ZoneId.systemDefault()).toInstant());
        Date endTime = Date.from(endResult.atZone(ZoneId.systemDefault()).toInstant());

        uiReportRunner.byReportCode("order")
                .addParam("startTime", startTime)
                .addParam("endTime", endTime)
                .runAndShow();
    }

}