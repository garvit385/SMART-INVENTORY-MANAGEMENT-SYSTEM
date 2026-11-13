package com.smartstock.ui;

import com.smartstock.model.Admin;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

public class LoginFrame extends JFrame {
  private final AppContext ctx;
  private final JTextField username = UI.field("Username", "", 24);
  private final JPasswordField password = new JPasswordField(24);
  private final JLabel message = UI.label(" ", 13, Theme.RED, false);
  private final JButton login;

  public LoginFrame(AppContext ctx) {
    super("SmartStock | Sign in");
    this.ctx = ctx;
    setName("login-frame");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(950, 650));
    setSize(1100, 720);
    setLocationRelativeTo(null);
    JPanel root = new JPanel(new GridLayout(1, 2));
    JPanel brand = new JPanel(new BorderLayout());
    brand.setBackground(Theme.NAVY);
    brand.setBorder(BorderFactory.createEmptyBorder(55, 48, 40, 40));
    JLabel logo = UI.label("  SmartStock", 30, Color.WHITE, true);
    logo.setIcon(new AppIcon("logo", Theme.TEAL, 40));
    brand.add(logo, BorderLayout.NORTH);
    JPanel story = UI.stack();
    story.add(UI.label("Your inventory.", 34, Color.WHITE, true));
    story.add(UI.label("Under control.", 34, Color.WHITE, true));
    story.add(Box.createVerticalStrut(22));
    story.add(
        UI.label(
            "One workspace for products, stock and sales.", 14, new Color(185, 200, 225), false));
    story.add(Box.createVerticalStrut(36));
    for (String text :
        new String[] {
          "Live inventory and low-stock alerts",
          "Reliable billing and stock tracking",
          "Clear reports. Better decisions."
        }) {
      story.add(UI.label("—  " + text, 14, new Color(216, 226, 241), false));
      story.add(Box.createVerticalStrut(18));
    }
    JPanel wrap = new JPanel(new GridBagLayout());
    wrap.setOpaque(false);
    wrap.add(story);
    brand.add(wrap, BorderLayout.CENTER);
    brand.add(
        UI.label("SMART INVENTORY MANAGEMENT SYSTEM  /  1.0", 10, new Color(153, 175, 205), false),
        BorderLayout.SOUTH);
    root.add(brand);
    JPanel right = new JPanel(new GridBagLayout());
    right.setBackground(Color.WHITE);
    JPanel form = UI.stack();
    form.setPreferredSize(new Dimension(340, 440));
    form.add(UI.label("WELCOME BACK", 11, Theme.BLUE, true));
    form.add(Box.createVerticalStrut(12));
    form.add(UI.label("Sign in to SmartStock", 27, Theme.TEXT, true));
    form.add(Box.createVerticalStrut(10));
    form.add(UI.label("Enter your administrator credentials to continue.", 13, Theme.MUTED, false));
    form.add(Box.createVerticalStrut(30));
    password.setName("Password");
    password.setPreferredSize(new Dimension(300, 40));
    password.setMaximumSize(new Dimension(500, 40));
    username.setMaximumSize(new Dimension(500, 40));
    form.add(new JLabel("Username"));
    form.add(Box.createVerticalStrut(8));
    form.add(username);
    form.add(Box.createVerticalStrut(18));
    form.add(new JLabel("Password"));
    form.add(Box.createVerticalStrut(8));
    form.add(password);
    JCheckBox show = new JCheckBox("Show password");
    show.setOpaque(false);
    char echo = password.getEchoChar();
    show.addActionListener(e -> password.setEchoChar(show.isSelected() ? (char) 0 : echo));
    form.add(Box.createVerticalStrut(10));
    form.add(show);
    form.add(Box.createVerticalStrut(12));
    form.add(message);
    form.add(Box.createVerticalStrut(12));
    login = UI.button("Sign in", true, this::authenticate);
    login.setMaximumSize(new Dimension(500, 43));
    form.add(login);
    form.add(Box.createVerticalStrut(20));
    form.add(UI.label("Local workspace • Secure PBKDF2 authentication", 11, Theme.MUTED, false));
    right.add(form);
    root.add(right);
    setContentPane(root);
    getRootPane().setDefaultButton(login);
  }

  private void authenticate() {
    if (username.getText().isBlank() || password.getPassword().length == 0) {
      message.setText("Please enter your username and password.");
      return;
    }
    login.setEnabled(false);
    message.setText("Signing in…");
    char[] secret = password.getPassword();
    String user = username.getText();
    new SwingWorker<Admin, Void>() {
      @Override
      protected Admin doInBackground() {
        return ctx.auth.login(user, secret);
      }

      @Override
      protected void done() {
        try {
          Admin admin = get();
          password.setText("");
          new MainDashboard(ctx, admin).setVisible(true);
          dispose();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          message.setText("Sign in interrupted.");
        } catch (ExecutionException e) {
          message.setText(e.getCause().getMessage());
          password.setText("");
        } finally {
          login.setEnabled(true);
        }
      }
    }.execute();
  }
}
