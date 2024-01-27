package Bot19;

public class Constants {
    static double[] attackBuff = {1.0, 1.05, 1.07, 1.1, 1.3, 1.35, 1.6}; // how much more damage per attack level
    static double[] attackCooldown = {1.0, 0.95, 0.93, 0.9, 0.8, 0.65, 0.4}; // how long attack cooldown is per attack level

    static double[] healBuff = {1.0, 1.03, 1.05, 1.07, 1.1, 1.15, 1.25};
    static double[] healCooldown = {1.0, 0.95, 0.9, 0.85, 0.85, 0.85, 0.75};

    static double[] buildCost = {1.0, 0.9, 0.85, 0.8, 0.7, 0.6, 0.5};
    static double[] buildCooldown = {1.0, 0.95, 0.9, 0.85, 0.8, 0.7, 0.5};
}