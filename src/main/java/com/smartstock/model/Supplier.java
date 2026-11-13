package com.smartstock.model;

public class Supplier {
  private int id;
  private String name, contactPerson, phone, email, address;

  public Supplier(
      int id, String name, String contactPerson, String phone, String email, String address) {
    this.id = id;
    this.name = name;
    this.contactPerson = contactPerson;
    this.phone = phone;
    this.email = email;
    this.address = address;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContactPerson() {
    return contactPerson;
  }

  public void setContactPerson(String v) {
    contactPerson = v;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String v) {
    phone = v;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String v) {
    email = v;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String v) {
    address = v;
  }

  @Override
  public String toString() {
    return name;
  }
}
