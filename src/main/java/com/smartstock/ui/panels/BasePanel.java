package com.smartstock.ui.panels;

import com.smartstock.ui.*;
import java.awt.*;
import javax.swing.*;

public abstract class BasePanel extends JPanel {
  protected final AppContext ctx;
  protected final MainDashboard host;
  protected final JPanel body = new JPanel(new BorderLayout(0, 20));
  protected final JPanel heading = new JPanel(new BorderLayout());

  protected BasePanel(AppContext ctx, MainDashboard host, String title, String subtitle) {
    super(new BorderLayout(0, 22));
    this.ctx = ctx;
    this.host = host;
    setBorder(BorderFactory.createEmptyBorder(28, 30, 24, 30));
    setBackground(Theme.BG);
    JPanel text = UI.stack();
    text.add(UI.label(title, 27, Theme.TEXT, true));
    text.add(Box.createVerticalStrut(7));
    text.add(UI.label(subtitle, 13, Theme.MUTED, false));
    heading.setOpaque(false);
    heading.add(text, BorderLayout.CENTER);
    add(heading, BorderLayout.NORTH);
    body.setOpaque(false);
    add(body, BorderLayout.CENTER);
  }

  public abstract void refresh();
}
