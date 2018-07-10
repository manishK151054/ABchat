package com.aapkabazzaar.abchat;

public class Requests {
    String request_type;

    public Requests()
    {

    }

    public Requests( String request_type) {
        this.request_type = request_type;
    }

    public String getReq_type() {
        return request_type;
    }

    public void setReq_type(String request_type) {
        this.request_type = request_type;
    }
}
