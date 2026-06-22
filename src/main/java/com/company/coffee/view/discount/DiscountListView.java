package com.company.coffee.view.discount;

import com.company.coffee.entity.Discount;
import com.company.coffee.entity.Product;
import com.company.coffee.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Route(value = "discounts", layout = MainView.class)
@ViewController(id = "Discount.list")
@ViewDescriptor(path = "discount-list-view.xml")
@LookupComponent("discountsDataGrid")
@DialogMode(width = "64em")
public class DiscountListView extends StandardListView<Discount> {
    @ViewComponent
    private DataGrid<Discount> discountsDataGrid;

    @ViewComponent
    private CollectionLoader<Discount> discountsDl;

    @Autowired
    private DataManager dataManager;

    @Subscribe("discountsDataGrid.removeAction")
    public void onDiscountsDataGridRemoveAction(final ActionPerformedEvent event) {
        Discount selectedDiscount = discountsDataGrid.getSingleSelectedItem();
        if (selectedDiscount == null) {
            return;
        }

        List<Product> relatedProducts = dataManager.load(Product.class)
                .query("select p from Product p where p.discount = :discount")
                .parameter("discount", selectedDiscount)
                .list();

        for (Product product : relatedProducts) {
            product.setDiscount(null);
        }

        SaveContext saveContext = new SaveContext()
                .saving(relatedProducts)
                .removing(selectedDiscount);

        dataManager.save(saveContext);

        discountsDl.load();
    }
}