package com.rsb.bubblepop;

public interface LoginHelper {
    boolean isLoggedIn();
    void Login(String name, String gender);
    void Logout();
}
