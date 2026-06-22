package com.company.coffee.view.signup;

import com.company.coffee.entity.User;
import com.company.coffee.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.textfield.JmixEmailField;
import io.jmix.flowui.component.textfield.JmixPasswordField;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

@Route(value = "signup")
@ViewController(id = "SignUpView")
@ViewDescriptor(path = "sign-up-view.xml")
@AnonymousAllowed
public class SignUpView extends StandardView {
    @Autowired
    private DataManager dataManager;
    @ViewComponent
    private TypedTextField<String> usernameField;
    @ViewComponent
    private JmixEmailField emailField;
    @ViewComponent
    private JmixPasswordField passwordField;
    @ViewComponent
    private JmixPasswordField confirmPasswordField;
    @ViewComponent
    private JmixFormLayout signUpForm;
    @ViewComponent
    private TypedTextField<String> phoneField;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private Notifications notifications;

    @Subscribe("signupBtn")
    public void onSignUp(ClickEvent<JmixButton> event) {
        String inputPhone = phoneField.getTypedValue();
        String existedPhone = dataManager.loadValue(
                        "select e.phone from User e where e.phone = :phone", String.class)
                .parameter("phone", inputPhone)
                .optional()
                .orElse(null);

        String inputMail = emailField.getValue();
        String existedMail = dataManager.loadValue(
                "select e.email from User e where e.email = :email", String.class)
                .parameter("email", inputMail)
                .optional()
                .orElse(null);

        if (existedPhone != null) {
            notifications.show("Phone number already exists");
            return;
        }

        if (existedMail != null) {
            notifications.show("E-mail already exists");
            return;
        }

        if (passwordField.getValue().equals(confirmPasswordField.getValue())) {
            User user = dataManager.create(User.class);
            user = dataManager.create(User.class);
            user.setUsername(usernameField.getTypedValue());
            user.setActive(true);
            user.setEmail(inputMail);
            user.setPassword("{noop}" + passwordField.getValue());
            user.setPhone(inputPhone);
            dataManager.unconstrained().save(user);
            RoleAssignmentEntity roleAssignmentEntity = dataManager.create(RoleAssignmentEntity.class);
            roleAssignmentEntity.setUsername(usernameField.getTypedValue());
            roleAssignmentEntity.setRoleCode("customer-role");
            roleAssignmentEntity.setRoleType("resource");
            dataManager.unconstrained().save(roleAssignmentEntity);
            viewNavigators.view(this, MainView.class).navigate();
        } else {
            notifications.show("Confirm Password do not match");
        }
    }
}