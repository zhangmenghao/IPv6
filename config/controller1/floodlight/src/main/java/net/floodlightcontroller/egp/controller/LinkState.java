package net.floodlightcontroller.egp.controller;


public class LinkState {

    private boolean link;

    public static final boolean UP = true;
    public static final boolean DOWN = false;

    public LinkState() {
        this.link = DOWN;
    }

    public void ChangeState() {
        this.link = !this.link;
    }

    public boolean isLink() {
        return link;
    }

    public void setLink(boolean link) {
        this.link = link;
    }
}
