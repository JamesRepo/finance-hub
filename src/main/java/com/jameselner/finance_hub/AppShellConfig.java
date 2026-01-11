package com.jameselner.finance_hub;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@PWA(name = "Finance Hub", shortName = "Finance Hub")
@Theme(variant = Lumo.DARK)
public class AppShellConfig implements AppShellConfigurator {
}
