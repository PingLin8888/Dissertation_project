package core;

import edu.princeton.cs.algs4.StdDraw;

import java.awt.*;

public class ChineseTextExample {
    public static void main(String[] args) {
        // Set up the canvas
        StdDraw.setCanvasSize(800, 600);
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);

        // Load a font that supports Chinese characters
        Font font = new Font("SimSun", Font.PLAIN, 24); // Change "SimSun" to your desired font
        StdDraw.setFont(font); // Set the font in StdDraw

        // Clear the canvas
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        // Draw Chinese text
        StdDraw.text(0.5, 0.5, "登录 / 创建个人资料 (P)"); // Example Chinese text
        StdDraw.text(0.5, 0.4, "退出 (Q)"); // Example Chinese text

        // Show the canvas
        StdDraw.show();
    }
}