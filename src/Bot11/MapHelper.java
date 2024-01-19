package Bot11;

import battlecode.common.MapLocation;

public class MapHelper {
    public static int WIDTH, HEIGHT;

    // returns the corresponding bit for a quadrant on the shared array at index 0
    // total bytecost: 18
    public static int getQuadrant(MapLocation pos) {
        if (pos.x <= WIDTH/2)
            if (pos.y <= HEIGHT/2)
                return 12;
            else
                return 11;
        else
            if (pos.y <= HEIGHT/2)
                return 9;
            else
                return 10;
    }

    public static MapLocation poseDecoder(int data) {
        return new MapLocation(data >> 8, data & 0xff);
    }

    public static int poseEncoder(MapLocation pose) {
        return (pose.x<<8)+pose.y;
    }
}
