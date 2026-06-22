package com.company.coffee.view.login;

import com.company.coffee.view.signup.SignUpView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.login.AbstractLogin.LoginEvent;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import io.jmix.core.CoreProperties;
import io.jmix.core.MessageTools;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.loginform.JmixLoginForm;
import io.jmix.flowui.kit.component.ComponentUtils;
import io.jmix.flowui.kit.component.loginform.JmixLoginI18n;
import io.jmix.flowui.view.*;
import io.jmix.securityflowui.authentication.AuthDetails;
import io.jmix.securityflowui.authentication.LoginViewSupport;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Route(value = "login")
@ViewController(id = "LoginView")
@ViewDescriptor(path = "login-view.xml")
public class LoginView extends StandardView implements LocaleChangeObserver {

    private static final Logger log = LoggerFactory.getLogger(LoginView.class);

    @Autowired
    private CoreProperties coreProperties;

    @Autowired
    private LoginViewSupport loginViewSupport;

    @Autowired
    private MessageTools messageTools;

    @ViewComponent
    private JmixLoginForm login;

    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private ViewNavigators viewNavigators;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Subscribe
    public void onInit(final InitEvent event) {
        initLocales();
    }

    private void initLocales() {
        LinkedHashMap<Locale, String> locales = coreProperties.getAvailableLocales().stream()
                .collect(Collectors.toMap(Function.identity(), messageTools::getLocaleDisplayName, (s1, s2) -> s1,
                        LinkedHashMap::new));

        ComponentUtils.setItemsMap(login, locales);

        login.setSelectedLocale(VaadinSession.getCurrent().getLocale());
    }

    @Subscribe("login")
    public void onLogin(final LoginEvent event) {
        try {
            loginViewSupport.authenticate(
                    AuthDetails.of(event.getUsername(), event.getPassword())
                            .withLocale(login.getSelectedLocale())
                            .withRememberMe(login.isRememberMe())
            );

            // xử lý session ở đây
//            ServletRequestAttributes attr =
//                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
//
//            HttpSession currentSession = attr.getRequest().getSession(false);
//
//            String currentSessionId = currentSession.getId();
//            for (Object principal : sessionRegistry.getAllPrincipals()) {
//
//                List<SessionInformation> sessions =
//                        sessionRegistry.getAllSessions(principal, false);
//
//                for (SessionInformation session : sessions) {
//
//                    if (!session.getSessionId().equals(currentSessionId)) {
//
//                        System.out.println("BEFORE=" + session.isExpired());
//
//                        session.expireNow();
//
//                        System.out.println("AFTER=" + session.isExpired());
//                    }
//                }
//            }
//            System.out.println("========== PRINCIPALS ==========");
//
//            for (Object principal : sessionRegistry.getAllPrincipals()) {
//
//                System.out.println("PRINCIPAL CLASS = " + principal.getClass());
//                System.out.println("PRINCIPAL = " + principal);
//
//                List<SessionInformation> sessions =
//                        sessionRegistry.getAllSessions(principal, false);
//
//                for (SessionInformation s : sessions) {
//                    System.out.println(
//                            "SESSION = " + s.getSessionId()
//                                    + ", EXPIRED = " + s.isExpired()
//                    );
//                }
//            }

        } catch (final BadCredentialsException |
                       DisabledException |
                       LockedException |
                       AccessDeniedException e) {

            log.warn("Login failed for user '{}': {}", event.getUsername(), e.toString());
            event.getSource().setError(true);
        }
    }

    @Override
    public void localeChange(final LocaleChangeEvent event) {
        UI.getCurrent().getPage().setTitle(messageBundle.getMessage("LoginView.title"));

        final JmixLoginI18n loginI18n = JmixLoginI18n.createDefault();

        final JmixLoginI18n.JmixForm form = new JmixLoginI18n.JmixForm();
        form.setTitle(messageBundle.getMessage("loginForm.headerTitle"));
        form.setUsername(messageBundle.getMessage("loginForm.username"));
        form.setPassword(messageBundle.getMessage("loginForm.password"));
        form.setSubmit(messageBundle.getMessage("loginForm.submit"));
        form.setForgotPassword(messageBundle.getMessage("loginForm.forgotPassword"));
        form.setRememberMe(messageBundle.getMessage("loginForm.rememberMe"));
        loginI18n.setForm(form);

        final LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
        errorMessage.setTitle(messageBundle.getMessage("loginForm.errorTitle"));
        errorMessage.setMessage(messageBundle.getMessage("loginForm.badCredentials"));
        errorMessage.setUsername(messageBundle.getMessage("loginForm.errorUsername"));
        errorMessage.setPassword(messageBundle.getMessage("loginForm.errorPassword"));
        loginI18n.setErrorMessage(errorMessage);

        login.setI18n(loginI18n);
    }

    @Subscribe("signupRedirect")
    public void onSignupRedirect(ClickEvent<?> event) {
        viewNavigators.view(this, SignUpView.class).navigate();
    }
}
