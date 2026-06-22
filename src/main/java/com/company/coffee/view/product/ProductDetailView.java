package com.company.coffee.view.product;

import com.company.coffee.entity.Discount;
import com.company.coffee.entity.Product;
import com.company.coffee.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.view.*;

@Route(value = "products/:id", layout = MainView.class)
@ViewController(id = "Product.detail")
@ViewDescriptor(path = "product-detail-view.xml")
@EditedEntityContainer("productDc")
public class ProductDetailView extends StandardDetailView<Product> {
    @ViewComponent
    private EntityComboBox<Discount> discountField;

    @Subscribe
    public void onInit(final InitEvent event) {
        discountField.setItemLabelGenerator(discount ->
                discount.getName() + " - " + discount.getPercentage() + "%"
        );
    }
}