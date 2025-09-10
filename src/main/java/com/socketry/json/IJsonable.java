package com.socketry.json;

public interface IJsonable {
    public String toJson();
    public static IJsonable fromJson(String json) {
        return null;
    }
}
