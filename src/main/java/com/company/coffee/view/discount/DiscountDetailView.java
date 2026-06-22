package com.company.coffee.view.discount;

import com.company.coffee.entity.Discount;
import com.company.coffee.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "discounts/:id", layout = MainView.class)
@ViewController(id = "Discount.detail")
@ViewDescriptor(path = "discount-detail-view.xml")
@EditedEntityContainer("discountDc")
public class DiscountDetailView extends StandardDetailView<Discount> {
}