package vn.pmgteam.yanase.gui;

public interface IClickable {
    boolean isMouseOver(float mx, float my);
    void onMousePressed(float mx, float my, int button);
    void onMouseReleased(float mx, float my, int button);
}